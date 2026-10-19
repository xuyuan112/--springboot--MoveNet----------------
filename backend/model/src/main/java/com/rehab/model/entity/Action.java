package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准动作
 */
@Schema(description = "标准动作")
@Data
@TableName("action")
public class Action {

    /** 动作ID */
    @Schema(description = "动作ID")
    @TableId(value = "action_id", type = IdType.AUTO)
    private Long actionId;

    /** 动作名称 */
    @Schema(description = "动作名称")
    private String name;

    /** 目标康复部位编码 */
    @Schema(description = "目标康复部位编码")
    private String targetPartCode;

    /** 难度：EASY/MEDIUM/HARD */
    @Schema(description = "难度：EASY/MEDIUM/HARD")
    private String difficulty;

    /** 动作说明 */
    @Schema(description = "动作说明")
    private String description;

    /** 状态：DRAFT/PUBLISHED/DISABLED */
    @Schema(description = "状态：DRAFT/PUBLISHED/DISABLED")
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
