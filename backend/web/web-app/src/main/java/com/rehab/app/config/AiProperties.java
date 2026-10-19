package com.rehab.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 问答配置（P5-B4，rehab.ai.*）。
 *
 * <p>provider 两种模式：</p>
 * <ul>
 *   <li>{@code mock}：内置确定性演示应答（离线/开发环境，不依赖外部服务）；</li>
 *   <li>{@code openai}：调用 OpenAI 兼容 chat/completions 接口（生产，密钥经环境变量注入）。</li>
 * </ul>
 * <p>无论何种模式，AI 输出都不得包含诊断/处方/风险结论，服务端强制附加免责声明。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "rehab.ai")
public class AiProperties {

    /** 提供方：mock / openai */
    private String provider = "mock";

    /** OpenAI 兼容服务根地址（如 https://api.openai.com/v1），openai 模式必填 */
    private String baseUrl = "";

    /** API Key（生产通过环境变量 REHAB_AI_API_KEY 注入，禁止提交密钥） */
    private String apiKey = "";

    /** 模型名 */
    private String model = "gpt-4o-mini";

    /** 上游请求超时（秒） */
    private int timeoutSeconds = 30;

    /** 单用户每日问答次数上限，超限返回 429 RATE_LIMITED */
    private int dailyLimitPerUser = 20;

    /** 单个问题最大长度（字符） */
    private int maxQuestionLength = 500;
}
