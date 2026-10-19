package com.rehab.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 知识问答请求（P5-B4）。
 *
 * <p>仅支持纯文本提问；不持久化历史；问题长度受限防滥用。</p>
 */
@Data
@Schema(description = "AI 知识问答请求")
public class AiQaRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "问题内容（一般性健康/康复知识）")
    @NotBlank(message = "问题内容不能为空")
    @Size(max = 500, message = "问题内容不能超过 500 字")
    private String question;
}
