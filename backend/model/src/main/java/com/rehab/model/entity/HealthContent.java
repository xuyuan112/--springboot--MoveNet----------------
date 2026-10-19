package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 公共健康内容
 */
@Schema(description = "公共健康内容")
@Data
@TableName("content")
public class HealthContent {

    /** 内容ID */
    @Schema(description = "内容ID")
    @TableId(value = "content_id", type = IdType.AUTO)
    private Long contentId;

    /** 标题 */
    @Schema(description = "标题")
    private String title;

    /** 内容类型：ARTICLE文章/VIDEO视频/GUIDE指南 */
    @Schema(description = "内容类型：ARTICLE文章/VIDEO视频/GUIDE指南")
    private String contentType;

    /** 关联康复部位编码 */
    @Schema(description = "关联康复部位编码")
    private String targetPartCode;

    /** 目标人群标签 */
    @Schema(description = "目标人群标签")
    private String targetGroup;

    /** 摘要 */
    @Schema(description = "摘要")
    private String summary;

    /** 正文 */
    @Schema(description = "正文")
    private String contentBody;

    /** 作者用户ID */
    @Schema(description = "作者用户ID")
    private Long authorId;

    /** 状态：DRAFT/PENDING_REVIEW/PUBLISHED/OFFLINE */
    @Schema(description = "状态：DRAFT/PENDING_REVIEW/PUBLISHED/OFFLINE")
    private String status;

    /** 审核人用户ID */
    @Schema(description = "审核人用户ID")
    private Long reviewerId;

    /** 审核意见 */
    @Schema(description = "审核意见")
    private String reviewRemark;

    /** 生效时间 */
    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    /** 失效时间 */
    @Schema(description = "失效时间")
    private LocalDateTime validTo;

    /** 发布时间 */
    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

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
