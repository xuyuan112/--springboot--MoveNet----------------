package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 咨询会话
 */
@Schema(description = "咨询会话")
@Data
@TableName("consultation")
public class Consultation {

    /** 会话ID */
    @Schema(description = "会话ID")
    @TableId(value = "consultation_id", type = IdType.AUTO)
    private Long consultationId;

    /** 患者用户ID */
    @Schema(description = "患者用户ID")
    private Long patientId;

    /** 治疗师用户ID */
    @Schema(description = "治疗师用户ID")
    private Long therapistId;

    /** 患者治疗师绑定ID */
    @Schema(description = "患者治疗师绑定ID")
    private Long bindingId;

    /** 状态：OPEN/CLOSED */
    @Schema(description = "状态：OPEN/CLOSED")
    private String status;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 关闭时间 */
    @Schema(description = "关闭时间")
    private LocalDateTime closedAt;
}
