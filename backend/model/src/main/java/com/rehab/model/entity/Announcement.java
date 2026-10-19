package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 平台公告
 */
@Schema(description = "平台公告")
@Data
@TableName("announcement")
public class Announcement {

    /** 公告ID */
    @Schema(description = "公告ID")
    @TableId(value = "announcement_id", type = IdType.AUTO)
    private Long announcementId;

    /** 标题 */
    @Schema(description = "标题")
    private String title;

    /** 公告正文 */
    @Schema(description = "公告正文")
    private String content;

    /** 可见范围：ALL全部/PATIENT患者/THERAPIST治疗师 */
    @Schema(description = "可见范围：ALL全部/PATIENT患者/THERAPIST治疗师")
    private String scope;

    /** 状态：DRAFT/PUBLISHED/OFFLINE */
    @Schema(description = "状态：DRAFT/PUBLISHED/OFFLINE")
    private String status;

    /** 展示开始时间 */
    @Schema(description = "展示开始时间")
    private LocalDateTime startTime;

    /** 展示结束时间 */
    @Schema(description = "展示结束时间")
    private LocalDateTime endTime;

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
