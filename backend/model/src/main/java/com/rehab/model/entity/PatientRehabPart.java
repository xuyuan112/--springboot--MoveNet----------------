package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 患者康复部位（多对多）
 */
@Schema(description = "患者康复部位（多对多）")
@Data
@TableName("patient_rehab_part")
public class PatientRehabPart {

    /** 登记ID */
    @Schema(description = "登记ID")
    @TableId(value = "patient_rehab_part_id", type = IdType.AUTO)
    private Long patientRehabPartId;

    /** 患者用户ID(user_account.user_id) */
    @Schema(description = "患者用户ID(user_account.user_id)")
    private Long patientId;

    /** 康复部位编码 */
    @Schema(description = "康复部位编码")
    private String rehabPartCode;

    /** 侧别 LEFT/RIGHT/BILATERAL */
    @Schema(description = "侧别 LEFT/RIGHT/BILATERAL")
    private String side;

    /** 是否主康复部位 1是0否 */
    @Schema(description = "是否主康复部位 1是0否")
    private Integer primaryFlag;

    /** ACTIVE/RESOLVED */
    @Schema(description = "ACTIVE/RESOLVED")
    private String status;

    /** 确诊或受损日期 */
    @Schema(description = "确诊或受损日期")
    private LocalDate diagnosedAt;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /** 逻辑删除：0正常1已删 */
    @Schema(description = "逻辑删除：0正常1已删")
    @TableLogic
    private Integer deleted;
}
