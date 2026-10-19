package com.rehab.model.dto;

import com.rehab.model.enums.ActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 动作版本创建/修改请求（POST /admin/actions/{actionId}/versions、PUT /admin/action-versions/{versionId}）
 */
@Data
@Schema(description = "动作版本创建/修改请求（POST /admin/actions/{actionId}/versions、PUT /admin/action-versions/{versionId}）")
public class ActionVersionSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 动作类型：ROM 关节活动度 / STRENGTH 力量 / BALANCE 平衡 */
    @Schema(description = "动作类型：ROM / STRENGTH / BALANCE", example = "ROM")
    @NotNull(message = "动作类型不能为空")
    private ActionType actionType;

    /** 姿态引擎标识（如 movenet-lightning-v4） */
    @Schema(description = "姿态引擎标识", example = "movenet-lightning-v4")
    @NotBlank(message = "姿态引擎标识不能为空")
    @Size(max = 100, message = "引擎标识最长 100 个字符")
    private String poseEngineId;

    /** 算法配置 JSON（采样率、平滑窗口等），原样存储 */
    @Schema(description = "算法配置 JSON（采样率、平滑窗口等）")
    @Size(max = 4000, message = "算法配置最长 4000 个字符")
    private String algoConfig;

    /** 运动特征签名 JSON（发布时校验），原样存储 */
    @Schema(description = "运动特征签名 JSON")
    @Size(max = 8000, message = "运动签名最长 8000 个字符")
    private String motionSignature;

}
