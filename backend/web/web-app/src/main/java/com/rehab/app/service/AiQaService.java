package com.rehab.app.service;

import com.rehab.app.vo.AiQaVO;
import cn.hutool.json.JSONObject;

/**
 * AI 知识问答服务（P5-B4，接口文档 6）。
 *
 * <p>不持久化会话历史；仅回答一般性知识，不得输出诊断/处方/风险结论/实时计数/计划修改；
 * AI 依赖异常返回 502 UPSTREAM_ERROR；单用户每日限流超限返回 429。</p>
 */
public interface AiQaService {

    /**
     * 患者知识问答。
     *
     * @param question 问题文本（1~500 字）
     * @return 回答 + 免责声明
     */
    AiQaVO ask(String question);

    /**
     * 基于报告统计快照生成 AI 小结草稿（仅写入 assessment_report.ai_draft，
     * 不绕过治疗师审核；供调度预生成调用，失败返回 null 不阻断流程）。
     */
    String generateReportDraft(JSONObject snapshot);
}
