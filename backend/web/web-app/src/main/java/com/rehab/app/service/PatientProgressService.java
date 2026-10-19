package com.rehab.app.service;

import com.rehab.app.vo.PatientProgressPageVO;
import com.rehab.app.vo.PatientProgressVO;
import com.rehab.app.vo.PatientTrendPointVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 患者进度/趋势服务（P5-B1，接口文档 3.4）。
 */
public interface PatientProgressService {

    /**
     * 三级完成度（日/计划/动作项）+ 任务完成明细分页。
     *
     * <p>日级聚合走 Redis 今日缓存（cache:progress:today:{userId}），训练提交/撤回后失效。</p>
     *
     * @param page   页码
     * @param size   每页条数
     * @param from   任务日期范围起（含，可空）
     * @param to     任务日期范围止（含，可空）
     * @param planId 计划筛选（可空）
     */
    PatientProgressPageVO getProgress(Integer page, Integer size, LocalDate from, LocalDate to, Long planId);

    /**
     * 得分/达标率/训练次数趋势（排除 INSUFFICIENT 记录）。
     *
     * @param granularity     DAY（自然日）/ WEEK（ISO 周，周一为界）
     * @param actionVersionId 动作版本筛选（可空）
     */
    List<PatientTrendPointVO> getTrends(LocalDate from, LocalDate to, Long actionVersionId, String granularity);

    // ==================== P7 治疗师端复用入口（显式患者维度） ====================

    /**
     * 指定患者的三级完成度（不含任务明细分页；治疗师端 /patients/{id}/completion 用）。
     *
     * @param patientUserId 患者用户ID（调用方需先完成绑定校验）
     */
    PatientProgressVO getCompletionForPatient(Long patientUserId, Long planId, LocalDate from, LocalDate to);

    /**
     * 指定患者的趋势（含 ROM/保持率/姿态质量扩展维度；治疗师端 /patients/{id}/trends 用）。
     *
     * @param patientUserId 患者用户ID（调用方需先完成绑定校验）
     */
    List<PatientTrendPointVO> getTrendsForPatient(Long patientUserId, LocalDate from, LocalDate to,
                                                  Long actionVersionId, String granularity);
}
