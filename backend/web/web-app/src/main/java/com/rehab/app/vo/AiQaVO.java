package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 知识问答响应（P5-B4）。
 *
 * <p>不持久化会话历史；响应强制携带免责声明，AI 不得输出诊断/处方/风险结论。</p>
 */
@Data
@AllArgsConstructor
@Schema(description = "AI 知识问答响应")
public class AiQaVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "回答内容")
    private String answer;

    @Schema(description = "免责声明（固定文案）")
    private String disclaimer;
}
