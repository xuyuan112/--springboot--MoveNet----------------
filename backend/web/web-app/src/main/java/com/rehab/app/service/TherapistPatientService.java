package com.rehab.app.service;

import com.rehab.app.vo.TherapistAlertVO;
import com.rehab.app.vo.TherapistPatientDetailVO;
import com.rehab.app.vo.TherapistPatientTaskVO;
import com.rehab.app.vo.TherapistPatientVO;
import com.rehab.common.result.PageResult;

import java.time.LocalDate;

/**
 * 治疗师端患者管理服务（P6-B2，接口文档 5.1）。
 *
 * <p>对象级权限：所有方法入口校验当前治疗师与患者的 {@code patient_therapist}
 * 绑定为 ACTIVE（不存在/已解除一律 403 OWNERSHIP_DENIED）。</p>
 */
public interface TherapistPatientService {

    /**
     * 分页查询当前有效绑定患者。
     *
     * @param keyword    姓名/手机号模糊匹配（可空）
     * @param partCode   主康复部位编码精确匹配（可空）
     * @param planStatus 按最近一次计划状态过滤（可空）
     */
    PageResult<TherapistPatientVO> listPatients(Integer page, Integer size,
                                                String keyword, String partCode, String planStatus);

    /** 患者详情：档案（病史等治疗必需字段）+ 绑定信息 + 风险提示；手机号脱敏。 */
    TherapistPatientDetailVO getPatientDetail(Long patientId);

    /**
     * 分页查询患者每日任务（仅限本人创建计划产生的任务）。
     *
     * @param from   任务日期起（含，可空）
     * @param to     任务日期止（含，可空）
     * @param status 任务状态 PENDING/IN_PROGRESS/COMPLETED/SKIPPED/EXPIRED（可空）
     */
    PageResult<TherapistPatientTaskVO> listPatientTasks(Long patientId, LocalDate from, LocalDate to,
                                                        String status, Integer page, Integer size);

    /**
     * 查询患者训练异常提示（固定近 7 日窗口，仅提示不构成医学诊断）：
     * 单次得分低于 60、近 3 天有任务均未完成、训练备注/咨询消息提及疼痛。
     */
    PageResult<TherapistAlertVO> listPatientAlerts(Long patientId, Integer page, Integer size);
}
