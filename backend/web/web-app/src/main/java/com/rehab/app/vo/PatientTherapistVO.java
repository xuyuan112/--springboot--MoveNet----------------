package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 患者当前有效负责治疗师视图对象（无绑定时接口返回 data:null）
 */
@Schema(description = "患者当前有效负责治疗师视图对象")
@Data
public class PatientTherapistVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 绑定关系ID */
    @Schema(description = "绑定关系ID")
    private Long bindingId;

    /** 治疗师用户ID */
    @Schema(description = "治疗师用户ID")
    private Long therapistId;

    /** 治疗师姓名 */
    @Schema(description = "治疗师姓名")
    private String name;

    /** 职称 */
    @Schema(description = "职称")
    private String title;

    /** 专长 */
    @Schema(description = "专长")
    private String specialty;

    /** 简介 */
    @Schema(description = "简介")
    private String introduction;

    /** 关系类型：PRIMARY主治/SECONDARY协作 */
    @Schema(description = "关系类型：PRIMARY主治/SECONDARY协作")
    private String relationType;

    /** 绑定开始时间 */
    @Schema(description = "绑定开始时间")
    private LocalDateTime startAt;
}
