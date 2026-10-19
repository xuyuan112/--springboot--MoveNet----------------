package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 患者进度三级完成度聚合（P5-B1，接口文档 3.4）。
 *
 * <p>三级口径：</p>
 * <ul>
 *   <li>日：当日已完成任务数 / 当日应执行任务数；</li>
 *   <li>计划：已完成任务数 / 累计已生成任务数（按查询范围）；</li>
 *   <li>动作项：按计划动作项聚合的已完成 / 已生成任务数。</li>
 * </ul>
 * <p>达标口径与任务进度一致：排除 INSUFFICIENT 记录；SKIPPED 任务不计入应执行。</p>
 */
@Data
@Schema(description = "患者进度三级完成度")
public class PatientProgressVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "日级完成度（当日）")
    private DayLevel day;

    @Schema(description = "计划级完成度（查询范围内累计）")
    private PlanLevel plan;

    @Schema(description = "动作项级完成度列表")
    private List<ActionItemLevel> actionItems;

    @Data
    @Schema(description = "日级完成度")
    public static class DayLevel implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "统计日期（当日）")
        private String date;

        @Schema(description = "当日已完成任务数")
        private Integer doneTasks;

        @Schema(description = "当日应执行任务数（不含 SKIPPED）")
        private Integer totalTasks;

        @Schema(description = "完成百分比")
        private BigDecimal percent;
    }

    @Data
    @Schema(description = "计划级完成度")
    public static class PlanLevel implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "已完成任务数")
        private Integer completedTasks;

        @Schema(description = "累计已生成任务数（不含 SKIPPED）")
        private Integer totalTasks;

        @Schema(description = "完成百分比")
        private BigDecimal percent;
    }

    @Data
    @Schema(description = "动作项级完成度")
    public static class ActionItemLevel implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "计划项ID")
        private Long planItemId;

        @Schema(description = "动作名称")
        private String actionName;

        @Schema(description = "所属计划名称")
        private String planName;

        @Schema(description = "已完成任务数")
        private Integer completedTasks;

        @Schema(description = "已生成任务数（不含 SKIPPED）")
        private Integer totalTasks;

        @Schema(description = "完成百分比")
        private BigDecimal percent;
    }
}
