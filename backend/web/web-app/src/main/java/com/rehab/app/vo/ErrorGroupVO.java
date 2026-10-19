package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 训练纠错维度分布项（由 training_error_log 按维度聚合派生）。
 */
@Schema(description = "训练纠错维度分布项")
@Data
public class ErrorGroupVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 评估维度 */
    @Schema(description = "评估维度", example = "AMPLITUDE")
    private String dimension;

    /** 该维度纠错区间数 */
    @Schema(description = "该维度纠错区间数", example = "2")
    private Long count;
}
