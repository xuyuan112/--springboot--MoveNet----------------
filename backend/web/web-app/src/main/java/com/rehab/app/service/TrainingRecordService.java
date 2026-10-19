package com.rehab.app.service;

import com.rehab.app.vo.TrainingRecordDetailVO;
import com.rehab.app.vo.TrainingRecordSubmitVO;
import com.rehab.app.vo.TrainingRecordVO;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.TrainingRecordSubmitRequest;

import java.time.LocalDate;

/**
 * 患者端训练记录服务（P4-B2）：提交（clientRequestId 幂等）、分页、详情、撤回。
 */
public interface TrainingRecordService {

    /**
     * 提交训练汇总：五重归属校验（患者/计划/计划项/每日任务/动作版本）、
     * 指标范围校验（422 METRIC_INVALID）、引擎一致性校验（422 ENGINE_VERSION_MISMATCH）；
     * 有效帧占比不足时记录落库为 INSUFFICIENT 且不计入任务进度；
     * 事务内同时写入训练记录、纠错日志并更新任务进度；相同 clientRequestId 返回首次结果。
     */
    TrainingRecordSubmitVO submitTrainingRecord(TrainingRecordSubmitRequest request);

    /** 分页查询本人训练记录（按日期/计划/动作版本筛选） */
    PageResult<TrainingRecordVO> listMyRecords(Integer page, Integer size, LocalDate from, LocalDate to,
                                               Long planId, Long actionVersionId);

    /** 查询单次训练详情（含分项评分、关键指标与纠错摘要；不含原始视频与逐帧关键点） */
    TrainingRecordDetailVO getMyRecordDetail(Long recordId);

    /** 撤回训练记录（软删除 + 原因必填）；已纳入评估报告统计周期的记录不可撤回（409） */
    void withdrawRecord(Long recordId, String reason);
}
