package com.rehab.app.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rehab.app.config.AiProperties;
import com.rehab.app.service.AiQaService;
import com.rehab.app.vo.AiQaVO;
import com.rehab.common.constant.Constants;
import com.rehab.common.constant.RedisKeyConstants;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI 知识问答服务实现（P5-B4）。
 *
 * <p>双 provider：</p>
 * <ul>
 *   <li>{@code mock}：内置确定性演示应答（按关键词归类的一般性健康知识，不含诊断/处方）；</li>
 *   <li>{@code openai}：OpenAI 兼容 chat/completions（base-url/api-key/model 可配）。</li>
 * </ul>
 * <p>安全约束：仅一般性知识；系统提示词 + 免责声明双层约束；不持久化任何会话历史；
 * 单用户每日限流（Redis INCR，超限 429）；上游异常 502/504。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQaServiceImpl implements AiQaService {

    /** 患者问答系统提示词：约束输出边界（需求 6 / 接口文档 6） */
    private static final String QA_SYSTEM_PROMPT = """
            你是康复训练平台的健康知识助手。你只能回答一般性健康与康复训练常识 \
            （如热身与放松方法、训练节奏、呼吸配合、休息恢复等通识内容）。\
            你不得给出任何诊断结论、处方或用药建议、疾病风险判断、实时动作计数，\
            也不得建议修改患者的康复计划。回答使用中文，控制在 300 字以内，语气友好。\
            如果用户的问题涉及具体病情或需要个体化判断，请建议其咨询自己的治疗师或医生。""";

    /** 报告小结生成系统提示词：仅基于给定统计数据撰写摘要，不得编造或给出诊断 */
    private static final String REPORT_SYSTEM_PROMPT = """
            你是康复训练平台的报告助手。请严格根据用户给出的统计数据，用中文撰写一段 \
            120 字以内的患者康复训练阶段小结：概述训练完成情况、得分水平与需要关注的维度。\
            只描述数据事实与一般性训练建议，不得给出诊断、风险结论或治疗处方。""";

    private final AiProperties aiProperties;
    private final StringRedisTemplate stringRedisTemplate;

    /** 懒建 RestClient（避免 mock 模式无谓初始化；超时取自配置） */
    private volatile RestClient restClient;

    @Override
    public AiQaVO ask(String question) {
        Long userId = SecurityUtils.getUserId();
        checkRateLimit(userId);
        String safeQuestion = question.trim();
        String answer = "mock".equalsIgnoreCase(aiProperties.getProvider())
                ? mockAnswer(safeQuestion)
                : callChatApi(QA_SYSTEM_PROMPT, safeQuestion);
        return new AiQaVO(answer, Constants.MEDICAL_DISCLAIMER);
    }

    @Override
    public String generateReportDraft(JSONObject snapshot) {
        try {
            if ("mock".equalsIgnoreCase(aiProperties.getProvider())) {
                return mockReportDraft(snapshot);
            }
            return callChatApi(REPORT_SYSTEM_PROMPT, snapshot.toStringPretty());
        } catch (Exception e) {
            // AI 小结仅为草稿增强：失败不阻断报告预生成
            log.warn("AI 报告小结生成失败（忽略，报告保持 DRAFT）：{}", e.getMessage());
            return null;
        }
    }

    // ==================== 私有辅助 ====================

    /** 单用户每日限流：Redis INCR，首次写入设 24h TTL，超限 429。 */
    private void checkRateLimit(Long userId) {
        String key = RedisKeyConstants.AI_QA_RATE_LIMIT + userId;
        Long count = null;
        try {
            count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, Duration.ofHours(24));
            }
        } catch (Exception e) {
            log.warn("AI 问答限流计数异常（降级放行）：{}", e.getMessage());
        }
        if (count != null && count > aiProperties.getDailyLimitPerUser()) {
            throw new BusinessException(ResultCode.RATE_LIMITED,
                    "今日 AI 问答次数已用完（上限 " + aiProperties.getDailyLimitPerUser() + " 次），请明天再试");
        }
    }

    /** OpenAI 兼容 chat/completions 调用。 */
    private String callChatApi(String systemPrompt, String userContent) {
        if (!StringUtils.hasText(aiProperties.getBaseUrl()) || !StringUtils.hasText(aiProperties.getApiKey())) {
            throw new BusinessException(ResultCode.UPSTREAM_ERROR, "AI 服务未配置，请联系管理员");
        }
        Map<String, Object> body = Map.of(
                "model", aiProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)),
                "temperature", 0.5,
                "max_tokens", 512,
                "stream", false);
        String url = aiProperties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        String raw;
        try {
            raw = restClient().post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (ResourceAccessException e) {
            log.warn("AI 上游请求失败：{}", e.getMessage());
            throw new BusinessException(ResultCode.UPSTREAM_TIMEOUT, "AI 服务响应超时，请稍后重试");
        } catch (Exception e) {
            log.warn("AI 上游请求异常：{}", e.getMessage());
            throw new BusinessException(ResultCode.UPSTREAM_ERROR, "AI 服务暂不可用，请稍后重试");
        }
        try {
            JSONObject resp = JSONUtil.parseObj(raw);
            JSONArray choices = resp.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException("choices 为空");
            }
            String content = choices.getJSONObject(0).getJSONObject("message").getStr("content");
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("content 为空");
            }
            return content.trim();
        } catch (Exception e) {
            log.warn("AI 上游响应解析失败：{}", e.getMessage());
            throw new BusinessException(ResultCode.UPSTREAM_ERROR, "AI 服务返回异常，请稍后重试");
        }
    }

    private RestClient restClient() {
        if (restClient == null) {
            synchronized (this) {
                if (restClient == null) {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    int timeoutMs = Math.max(1, aiProperties.getTimeoutSeconds()) * 1000;
                    factory.setConnectTimeout(timeoutMs);
                    factory.setReadTimeout(timeoutMs);
                    restClient = RestClient.builder().requestFactory(factory).build();
                }
            }
        }
        return restClient;
    }

    /**
     * 内置演示应答：按关键词归类的一般性常识，固定话术不含诊断/处方/风险结论。
     */
    private String mockAnswer(String question) {
        if (question.contains("疼") || question.contains("痛") || question.contains("麻")
                || question.contains("刺") || question.contains("肿胀")) {
            return "训练中出现明显疼痛或不适时，一般建议先暂停训练、适当休息观察，避免强行坚持。"
                    + "轻微的肌肉酸胀通常在休息后可自行缓解。如果不适持续存在或反复出现，"
                    + "请及时联系您的主责治疗师或医生进行面对面评估。";
        }
        if (question.contains("热身") || question.contains("准备活动")) {
            return "训练前一般建议进行 5 到 10 分钟的低强度热身，例如慢走、关节活动和动态拉伸，"
                    + "让身体逐步进入运动状态，有助于减少训练中的不适。热身后可以再进行几组慢速的适应动作。";
        }
        if (question.contains("放松") || question.contains("拉伸") || question.contains("整理")) {
            return "训练结束后进行 5 分钟左右的放松活动（如慢走、缓慢的静态拉伸）有助于身体恢复。"
                    + "拉伸时保持自然呼吸、不要憋气，每个动作以感到轻微牵拉感为宜，避免弹震式用力。";
        }
        if (question.contains("频率") || question.contains("多久") || question.contains("几次")
                || question.contains("次数")) {
            return "训练频率和强度通常由治疗师根据个人情况在康复计划中设定，建议按照计划执行，"
                    + "不要自行加量。感觉疲劳时应适当休息，循序渐进地完成目标次数更为重要。";
        }
        if (question.contains("呼吸")) {
            return "训练中一般建议保持自然平稳的呼吸，用力阶段呼气、还原阶段吸气，避免长时间憋气。"
                    + "如果训练中感到头晕或呼吸急促，应立即停止并休息。";
        }
        return "康复训练需要循序渐进、持之以恒。建议按照治疗师制定的计划坚持训练，"
                + "注意动作规范与呼吸配合，训练前后做好热身与放松。如有个性化问题，请咨询您的主责治疗师。";
    }

    /** mock 报告小结：基于统计快照的确定性文本。 */
    private String mockReportDraft(JSONObject snapshot) {
        int completed = snapshot.getInt("completedTasks", 0);
        int total = snapshot.getInt("totalTasks", 0);
        BigDecimal rate = snapshot.getBigDecimal("completionRate");
        BigDecimal avgScore = snapshot.getBigDecimal("avgTotalScore");
        int recordCount = snapshot.getInt("recordCount", 0);
        StringBuilder text = new StringBuilder();
        text.append("本周期共生成 ").append(total).append(" 项训练任务，完成 ").append(completed)
                .append(" 项（完成率 ").append(rate != null ? rate.stripTrailingZeros().toPlainString() : "0")
                .append("%），累计有效训练 ").append(recordCount).append(" 次");
        if (avgScore != null) {
            text.append("，平均得分 ").append(avgScore).append(" 分");
        }
        text.append("。");
        JSONObject scoreAverages = snapshot.getJSONObject("scoreAverages");
        if (scoreAverages != null && !scoreAverages.isEmpty()) {
            String weakest = null;
            BigDecimal lowest = null;
            for (String key : scoreAverages.keySet()) {
                BigDecimal value = scoreAverages.getBigDecimal(key);
                if (value != null && (lowest == null || value.compareTo(lowest) < 0)) {
                    lowest = value;
                    weakest = key;
                }
            }
            if (weakest != null) {
                text.append("相对而言，「").append(weakest).append("」维度得分偏低，可在训练中适当关注动作质量。");
            }
        }
        text.append("以上为数据摘要，具体评估请以治疗师审核意见为准。");
        return text.toString();
    }
}
