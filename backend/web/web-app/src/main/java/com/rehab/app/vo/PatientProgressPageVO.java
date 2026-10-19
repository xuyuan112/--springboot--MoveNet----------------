package com.rehab.app.vo;

import com.rehab.common.result.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 患者进度聚合响应（P5-B1）：三级完成度 + 任务完成明细分页。
 */
@Data
@Schema(description = "患者进度聚合响应")
public class PatientProgressPageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "三级完成度聚合")
    private PatientProgressVO summary;

    @Schema(description = "任务完成明细分页")
    private PageResult<PatientTaskProgressVO> records;
}
