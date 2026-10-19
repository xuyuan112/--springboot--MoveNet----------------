package com.rehab.model.dto;

import com.rehab.model.enums.TemplateSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标准动作模板创建请求（POST /admin/action-versions/{versionId}/templates，上传示范数据生成归一化特征）
 */
@Data
@Schema(description = "标准动作模板创建请求（POST /admin/action-versions/{versionId}/templates，上传示范数据生成归一化特征）")
public class ActionTemplateSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模板来源：THERAPIST 治疗师录制 / ALGO 算法生成 */
    @Schema(description = "模板来源：THERAPIST / ALGO", example = "THERAPIST")
    @NotNull(message = "模板来源不能为空")
    private TemplateSource sourceType;

    /** 采样帧率（fps） */
    @Schema(description = "采样帧率（fps）", example = "30")
    @DecimalMin(value = "0", inclusive = false, message = "帧率需大于 0")
    private BigDecimal frameRate;

    /** 帧数 */
    @Schema(description = "帧数", example = "180")
    @Min(value = 1, message = "帧数至少为 1")
    private Integer frameCount;

    /** 特征维度 */
    @Schema(description = "特征维度", example = "34")
    @Min(value = 1, message = "特征维度至少为 1")
    private Integer featureDim;

    /** 标准特征序列 JSON（归一化后） */
    @Schema(description = "标准特征序列 JSON（归一化后）")
    @NotBlank(message = "特征序列不能为空")
    @Size(max = 20000, message = "特征序列过长")
    private String featureSequence;

    /** 关键帧索引 JSON */
    @Schema(description = "关键帧索引 JSON")
    @Size(max = 4000, message = "关键帧索引过长")
    private String keyframeIndex;

    /** 采样样本数 */
    @Schema(description = "采样样本数", example = "5")
    @Min(value = 1, message = "样本数至少为 1")
    private Integer sampleCount;

}
