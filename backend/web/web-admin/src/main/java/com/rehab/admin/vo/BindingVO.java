package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医患绑定关系项（含双方姓名冗余，便于管理端列表直接展示）
 */
@Data
@Builder
@Schema(description = "医患绑定关系项")
public class BindingVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 绑定关系ID */
    @Schema(description = "绑定关系ID")
    private Long bindingId;

    /** 患者用户ID */
    @Schema(description = "患者用户ID")
    private Long patientId;

    /** 患者姓名 */
    @Schema(description = "患者姓名")
    private String patientName;

    /** 治疗师用户ID */
    @Schema(description = "治疗师用户ID")
    private Long therapistId;

    /** 治疗师姓名 */
    @Schema(description = "治疗师姓名")
    private String therapistName;

    /** 关系类型码：PRIMARY/SECONDARY */
    @Schema(description = "关系类型码：PRIMARY/SECONDARY")
    private String relationType;

    /** 关系类型中文名 */
    @Schema(description = "关系类型中文名")
    private String relationTypeName;

    /** 状态码：ACTIVE/ENDED */
    @Schema(description = "状态码：ACTIVE/ENDED")
    private String status;

    /** 状态中文名 */
    @Schema(description = "状态中文名")
    private String statusName;

    /** 绑定开始时间 */
    @Schema(description = "绑定开始时间")
    private LocalDateTime startAt;

    /** 绑定结束时间（空表示长期有效） */
    @Schema(description = "绑定结束时间（空表示长期有效）")
    private LocalDateTime endAt;

    /** 创建人用户ID */
    @Schema(description = "创建人用户ID")
    private Long createdBy;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
