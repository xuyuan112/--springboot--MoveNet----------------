package com.rehab.app.service;

/**
 * 评估报告预生成服务（P5-B5，调度第三步）。
 *
 * <p>按计划周期（ISO 周）聚合已完成任务与训练记录，预生成 DRAFT 报告并固化 data_snapshot；
 * AI 小结仅写入草稿字段。同计划同周期唯一（uk_report_period），重复执行幂等跳过。</p>
 */
public interface ReportGenerateService {

    /**
     * 为上一个完整 ISO 周预生成各计划 DRAFT 报告。
     *
     * @return 本次新建报告数
     */
    int generatePreviousWeekReports();

    /**
     * regenerate（P7-B4）：按指定计划与周期已冻结的任务/训练数据重算统计，
     * 生成新 DRAFT/AI_GENERATED 版本（version_no 递增），原版本保留。
     *
     * @param planId      计划ID（调用方需先完成归属校验）
     * @param periodStart 周期开始
     * @param periodEnd   周期结束
     * @return 新报告ID
     */
    Long regenerateForPlan(Long planId, java.time.LocalDate periodStart, java.time.LocalDate periodEnd);
}
