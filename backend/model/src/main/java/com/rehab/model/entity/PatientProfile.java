package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 患者档案
 */
@Schema(description = "患者档案")
@Data
@TableName("patient_profile")
public class PatientProfile {

    /** 患者档案ID */
    @Schema(description = "患者档案ID")
    @TableId(value = "patient_profile_id", type = IdType.AUTO)
    private Long patientProfileId;

    /** 用户账号ID */
    @Schema(description = "用户账号ID")
    private Long userId;

    /** 真实姓名 */
    @Schema(description = "真实姓名")
    private String realName;

    /** 性别：M男/F女/U未知 */
    @Schema(description = "性别：M男/F女/U未知")
    private String gender;

    /** 出生日期 */
    @Schema(description = "出生日期")
    private LocalDate birthDate;

    /** 身高(cm) */
    @Schema(description = "身高(cm)")
    private BigDecimal heightCm;

    /** 体重(kg) */
    @Schema(description = "体重(kg)")
    private BigDecimal weightKg;

    /** 既往病史 */
    @Schema(description = "既往病史")
    private String medicalHistory;

    /** 过敏史 */
    @Schema(description = "过敏史")
    private String allergyInfo;

    /** 禁忌症 */
    @Schema(description = "禁忌症")
    private String contraindication;

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
