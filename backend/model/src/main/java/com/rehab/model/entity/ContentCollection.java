package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 内容收藏
 */
@Schema(description = "内容收藏")
@Data
@TableName("content_collection")
public class ContentCollection {

    /** 收藏ID */
    @Schema(description = "收藏ID")
    @TableId(value = "collection_id", type = IdType.AUTO)
    private Long collectionId;

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;

    /** 内容ID */
    @Schema(description = "内容ID")
    private Long contentId;

    /** 收藏时间 */
    @Schema(description = "收藏时间")
    private LocalDateTime collectedAt;
}
