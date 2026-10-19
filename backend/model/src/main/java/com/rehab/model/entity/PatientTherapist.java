package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 患者治疗师关系
 */
@Schema(description = "患者治疗师关系")
@Data
@TableName("patient_therapist")
public class PatientTherapist {

    /** 绑定关系ID */
    @Schema(description = "绑定关系ID")
    @TableId(value = "binding_id", type = IdType.AUTO)
    private Long bindingId;

    /** 患者用户ID */
    @Schema(description = "患者用户ID")
    private Long patientId;

    /** 治疗师用户ID */
    @Schema(description = "治疗师用户ID")
    private Long therapistId;

    /** 关系类型：PRIMARY主治/SECONDARY协作 */
    @Schema(description = "关系类型：PRIMARY主治/SECONDARY协作")
    private String relationType;

    /** 状态：ACTIVE/ENDED */
    @Schema(description = "状态：ACTIVE/ENDED")
    private String status;

    /** 绑定开始时间 */
    @Schema(description = "绑定开始时间")
    private LocalDateTime startAt;

    /** 绑定结束时间 */
    @Schema(description = "绑定结束时间")
    private LocalDateTime endAt;

    /** 创建人用户ID */
    @Schema(description = "创建人用户ID")
    private Long createdBy;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
