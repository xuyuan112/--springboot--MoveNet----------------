package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 治疗师工作台聚合 VO（GET /therapist/dashboard）。
 *
 * <p>口径说明（首版）：</p>
 * <ul>
 *   <li>负责患者数：当前 ACTIVE 绑定的去重患者数；</li>
 *   <li>今日任务完成率：本人负责计划当日生成的 plan_daily_task 中 COMPLETED 占比，无任务时为 null；</li>
 *   <li>待审核报告数：本人计划下未生效报告（DRAFT/AI_GENERATED/REVIEWED）；</li>
 *   <li>待回复咨询数：OPEN 会话中存在患者发送且未读消息的会话数；</li>
 *   <li>今日低分训练数：当日 total_score&lt;60 的训练记录数（仅提示，不构成医学判断）。</li>
 * </ul>
 */
@Data
@Schema(description = "治疗师工作台聚合数据")
public class TherapistDashboardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "负责患者数（ACTIVE 绑定去重）")
    private Long patientCount;

    @Schema(description = "今日任务总数")
    private Long todayTaskTotal;

    @Schema(description = "今日已完成任务数")
    private Long todayTaskCompleted;

    @Schema(description = "今日任务完成率（0-100，保留一位小数；无任务为 null）")
    private Double todayCompletionRate;

    @Schema(description = "待审核报告数（未生效报告）")
    private Long pendingReportCount;

    @Schema(description = "待回复咨询数（存在患者未读消息的 OPEN 会话）")
    private Long pendingConsultCount;

    @Schema(description = "今日低分训练数（total_score<60，仅提示）")
    private Long lowScoreTodayCount;
}
