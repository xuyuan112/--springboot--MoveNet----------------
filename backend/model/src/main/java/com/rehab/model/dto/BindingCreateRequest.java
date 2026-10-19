package com.rehab.model.dto;

import com.rehab.model.enums.RelationType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 建立医患绑定请求（POST /admin/bindings，校验双方账号启用及主责冲突）
 */
@Data
@Schema(description = "建立医患绑定请求（POST /admin/bindings，校验双方账号启用及主责冲突）")
public class BindingCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 患者用户ID */
    @Schema(description = "患者用户ID", example = "1001")
    @NotNull(message = "患者ID不能为空")
    private Long patientId;

    /** 治疗师用户ID */
    @Schema(description = "治疗师用户ID", example = "2001")
    @NotNull(message = "治疗师ID不能为空")
    private Long therapistId;

    /** 关系类型：PRIMARY 主责 / SECONDARY 协作 */
    @Schema(description = "关系类型：PRIMARY 主责 / SECONDARY 协作", example = "PRIMARY")
    @NotNull(message = "关系类型不能为空")
    private RelationType relationType;

    /** 绑定生效时间（可选，默认立即生效） */
    @Schema(description = "绑定生效时间（可选，默认立即生效）")
    private LocalDateTime startAt;

    /** 绑定失效时间（可选，空表示长期有效） */
    @Schema(description = "绑定失效时间（可选，空表示长期有效）")
    private LocalDateTime endAt;

}
