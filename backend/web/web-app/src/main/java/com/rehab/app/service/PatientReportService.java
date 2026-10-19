package com.rehab.app.service;

import com.rehab.app.vo.PatientReportDetailVO;
import com.rehab.app.vo.PatientReportVO;
import com.rehab.common.result.PageResult;

/**
 * 患者评估报告服务（P5-B2，接口文档 3.4）。
 *
 * <p>患者仅可见 PUBLISHED（接口文档 EFFECTIVE）报告；DRAFT/AI_GENERATED/REVIEWED 一律 404；
 * 患者不可修改报告。</p>
 */
public interface PatientReportService {

    /** 分页查询本人有效报告（status 参数仅接受 EFFECTIVE，缺省即只查 PUBLISHED）。 */
    PageResult<PatientReportVO> pageMyReports(Integer page, Integer size, String status);

    /** 查询报告正文、统计数据与免责声明。 */
    PatientReportDetailVO getMyReport(Long reportId);
}
