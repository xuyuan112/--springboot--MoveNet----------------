package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 治疗师档案
 */
@Schema(description = "治疗师档案")
@Data
@TableName("therapist_profile")
public class TherapistProfile {

    /** 治疗师档案ID */
    @Schema(description = "治疗师档案ID")
    @TableId(value = "therapist_profile_id", type = IdType.AUTO)
    private Long therapistProfileId;

    /** 用户账号ID */
    @Schema(description = "用户账号ID")
    private Long userId;

    /** 真实姓名 */
    @Schema(description = "真实姓名")
    private String realName;

    /** 职称 */
    @Schema(description = "职称")
    private String title;

    /** 专长领域 */
    @Schema(description = "专长领域")
    private String specialty;

    /** 执业证书编号 */
    @Schema(description = "执业证书编号")
    private String licenseNo;

    /** 个人简介 */
    @Schema(description = "个人简介")
    private String introduction;

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
