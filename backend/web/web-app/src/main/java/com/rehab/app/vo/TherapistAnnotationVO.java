package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 治疗师端复核批注（P7-B1）：只增不改，不修改原始评分。
 */
@Data
@Schema(description = "治疗师端复核批注")
public class TherapistAnnotationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "批注ID")
    private Long annotationId;

    @Schema(description = "训练记录ID")
    private Long recordId;

    @Schema(description = "批注治疗师用户ID")
    private Long therapistId;

    @Schema(description = "批注治疗师姓名")
    private String therapistName;

    @Schema(description = "批注内容")
    private String annotation;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
