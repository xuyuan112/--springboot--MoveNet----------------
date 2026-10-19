package com.rehab.app.service;

import com.rehab.app.vo.TherapistReportDetailVO;
import com.rehab.app.vo.TherapistReportVO;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.ReportReviewRequest;
import com.rehab.model.dto.ReportSubmitRequest;

/**
 * 治疗师端评估报告服务（P7-B4，接口文档 5.4）。
 *
 * <p>状态机（枚举映射）：DRAFT/AI_GENERATED --submit--> REVIEWED --review:APPROVE--> PUBLISHED，
 * --review:REJECT--> DRAFT（comment 必填）。regenerate 按冻结数据生成新 DRAFT 版本，
 * 原版本保留。报告归属经 plan.therapist_id 校验（非本人 403）。</p>
 */
public interface TherapistReportService {

    /** 分页查询本人负责患者的报告（patientId/planId/status 过滤）。 */
    PageResult<TherapistReportVO> listReports(Integer page, Integer size, Long patientId, Long planId, String status);

    /** 报告详情：正文 + data_snapshot 统计 + AI 草稿 + 结论 + 免责声明。 */
    TherapistReportDetailVO getReportDetail(Long reportId);

    /** 补充结论并提交审核：DRAFT/AI_GENERATED→REVIEWED；REVIEWED 允许更新结论；PUBLISHED 409。 */
    TherapistReportDetailVO submit(Long reportId, ReportSubmitRequest request);

    /** 审核：APPROVE REVIEWED→PUBLISHED；REJECT→DRAFT（comment 必填 400，状态不符 422）。 */
    TherapistReportDetailVO review(Long reportId, ReportReviewRequest request);

    /** 按冻结数据重算生成新 DRAFT 版本（version_no 递增，原版本保留）。 */
    TherapistReportDetailVO regenerate(Long reportId);

    /** 分页查询同计划同周期的报告版本链（版本号倒序）。 */
    PageResult<TherapistReportVO> listVersions(Long reportId, Integer page, Integer size);
}
