package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 动作版本
 */
@Schema(description = "动作版本")
@Data
@TableName("action_version")
public class ActionVersion {

    /** 动作版本ID */
    @Schema(description = "动作版本ID")
    @TableId(value = "action_version_id", type = IdType.AUTO)
    private Long actionVersionId;

    /** 动作ID */
    @Schema(description = "动作ID")
    private Long actionId;

    /** 版本号 */
    @Schema(description = "版本号")
    private Integer versionNo;

    /** 动作类型：ROM关节活动度/STRENGTH力量/BALANCE平衡 */
    @Schema(description = "动作类型：ROM关节活动度/STRENGTH力量/BALANCE平衡")
    private String actionType;

    /** 算法配置（采样率、平滑窗口等），JSON 字符串 */
    @Schema(description = "算法配置（采样率、平滑窗口等），JSON 字符串")
    private String algoConfig;

    /** 运动特征签名，JSON 字符串 */
    @Schema(description = "运动特征签名，JSON 字符串")
    private String motionSignature;

    /** 姿态引擎标识（如 movenet-lightning-v4） */
    @Schema(description = "姿态引擎标识（如 movenet-lightning-v4）")
    private String poseEngineId;

    /** 状态：DRAFT/PUBLISHED/DEPRECATED */
    @Schema(description = "状态：DRAFT/PUBLISHED/DEPRECATED")
    private String status;

    /** 创建人用户ID */
    @Schema(description = "创建人用户ID")
    private Long createdBy;

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
