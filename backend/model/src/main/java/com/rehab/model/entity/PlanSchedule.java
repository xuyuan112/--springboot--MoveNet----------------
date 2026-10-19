package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 计划执行日程
 */
@Schema(description = "计划执行日程")
@Data
@TableName("plan_schedule")
public class PlanSchedule {

    /** 日程ID */
    @Schema(description = "日程ID")
    @TableId(value = "schedule_id", type = IdType.AUTO)
    private Long scheduleId;

    /** 计划ID */
    @Schema(description = "计划ID")
    private Long planId;

    /** 星期几：1周一 ... 7周日 */
    @Schema(description = "星期几：1周一 ... 7周日")
    private Integer weekday;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
