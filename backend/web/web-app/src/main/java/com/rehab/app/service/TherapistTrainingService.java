package com.rehab.app.service;

import com.rehab.app.vo.PatientProgressVO;
import com.rehab.app.vo.PatientTrendPointVO;
import com.rehab.app.vo.TherapistAnnotationVO;
import com.rehab.app.vo.TherapistErrorLogVO;
import com.rehab.app.vo.TherapistTrainingRecordDetailVO;
import com.rehab.app.vo.TherapistTrainingRecordVO;
import com.rehab.common.result.PageResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 治疗师端训练数据服务（P7-B1/P7-B3，接口文档 5.3）。
 *
 * <p>全部接口校验患者绑定（解除/非负责 403）；记录详情不含原始视频与逐帧关键点；
 * 批注只增不改原始评分；导出 CSV 仅汇总指标，不含病史/视频/关键点，导出走审计。</p>
 */
public interface TherapistTrainingService {

    /** 分页查询负责患者训练记录（patientId/planId/actionVersionId/from/to/poseQuality 过滤）。 */
    PageResult<TherapistTrainingRecordVO> listRecords(Integer page, Integer size, Long patientId, Long planId,
                                                      Long actionVersionId, LocalDate from, LocalDate to,
                                                      String poseQuality);

    /** 单次训练详情：指标、分项分（评分构成）、算法版本快照。 */
    TherapistTrainingRecordDetailVO getRecordDetail(Long recordId);

    /** 分页查询纠错日志（维度/阶段/严重度/偏差/帧号）。 */
    PageResult<TherapistErrorLogVO> listErrors(Long recordId, Integer page, Integer size);

    /** 分页查询复核批注。 */
    PageResult<TherapistAnnotationVO> listAnnotations(Long recordId, Integer page, Integer size);

    /** 添加复核批注（只增，不修改原始评分）。 */
    TherapistAnnotationVO addAnnotation(Long recordId, String annotation);

    /**
     * 导出训练汇总 CSV（UTF-8 BOM，便于 Excel 打开）。
     *
     * @return CSV 字节内容；列不含病史/原始视频/逐帧关键点
     */
    byte[] exportCsv(Long patientId, LocalDate from, LocalDate to);

    /** 指定患者趋势（得分/达标率/ROM/保持率/姿态质量，排除 INSUFFICIENT；先校验 ACTIVE 绑定）。 */
    List<PatientTrendPointVO> getPatientTrends(Long patientId, LocalDate from, LocalDate to, String granularity);

    /** 指定患者三级完成度（日/计划/动作项；先校验 ACTIVE 绑定）。 */
    PatientProgressVO getPatientCompletion(Long patientId, Long planId, LocalDate from, LocalDate to);
}
