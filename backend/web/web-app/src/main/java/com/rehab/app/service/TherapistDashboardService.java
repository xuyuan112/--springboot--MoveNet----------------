package com.rehab.app.service;

import com.rehab.app.vo.TherapistDashboardVO;

/**
 * 治疗师工作台聚合服务（P6-B1，GET /therapist/dashboard）。
 *
 * <p>口径：负责患者数=ACTIVE 绑定去重；今日任务/完成率=本人计划当日生成的
 * plan_daily_task；待审核报告=本人计划下未生效报告（DRAFT/AI_GENERATED/REVIEWED）；
 * 待回复咨询=OPEN 会话中存在患者发送且未读消息；低分训练=当日 total_score&lt;60。</p>
 */
public interface TherapistDashboardService {

    /** 聚合当前治疗师工作台五项指标。 */
    TherapistDashboardVO getDashboard();
}
