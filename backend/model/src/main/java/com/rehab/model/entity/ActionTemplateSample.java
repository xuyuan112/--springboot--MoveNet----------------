package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 动作模板采样记录
 */
@Schema(description = "动作模板采样记录")
@Data
@TableName("action_template_sample")
public class ActionTemplateSample {

    /** 采样记录ID */
    @Schema(description = "采样记录ID")
    @TableId(value = "template_sample_id", type = IdType.AUTO)
    private Long templateSampleId;

    /** 模板ID */
    @Schema(description = "模板ID")
    private Long templateId;

    /** 样本视频文件ID */
    @Schema(description = "样本视频文件ID")
    private Long fileId;

    /** 样本序号 */
    @Schema(description = "样本序号")
    private Integer sampleNo;

    /** 特征摘要（去重用） */
    @Schema(description = "特征摘要（去重用）")
    private String featureHash;

    /** 采集时间 */
    @Schema(description = "采集时间")
    private LocalDateTime capturedAt;

    /** 创建人用户ID */
    @Schema(description = "创建人用户ID")
    private Long createdBy;
}
