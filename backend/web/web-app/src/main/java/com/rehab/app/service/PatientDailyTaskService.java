package com.rehab.app.service;

import com.rehab.app.vo.PatientDailyTaskVO;
import com.rehab.app.vo.TaskActionSnapshotVO;
import com.rehab.common.result.PageResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 患者端每日训练任务服务。
 */
public interface PatientDailyTaskService {

    /**
     * 查询今日任务聚合列表。
     *
     * @return 今日任务列表
     */
    List<PatientDailyTaskVO> listTodayTasks();

    /**
     * 按日期、状态分页查询每日任务。
     *
     * @param page   页码，从 1 开始
     * @param size   每页条数
     * @param from   开始日期（可选）
     * @param to     结束日期（可选）
     * @param status 状态过滤（可选）
     * @return 分页结果
     */
    PageResult<PatientDailyTaskVO> listMyTasks(Integer page, Integer size, LocalDate from, LocalDate to, String status);

    /**
     * 查询单条任务详情及完成进度。
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    PatientDailyTaskVO getMyTaskDetail(Long taskId);

    /**
     * 开始训练，返回动作版本参数快照。
     *
     * @param taskId 任务ID
     * @param clientPoseEngineId 端侧浏览器本地姿态引擎标识（X-Pose-Engine-Id 请求头上送），
     *                           与动作版本 pose_engine_id 不一致时抛 422 ENGINE_VERSION_MISMATCH；
     *                           为空（旧端/直连探测）不拦截，端侧仍有同口径拦截
     * @return 动作参数快照
     */
    TaskActionSnapshotVO startTask(Long taskId, String clientPoseEngineId);

    /**
     * 暂停训练并记录原因。
     *
     * @param taskId 任务ID
     * @param reason 暂停原因
     */
    void pauseTask(Long taskId, String reason);

    /**
     * 主动结束/跳过任务，必须提交原因。
     *
     * @param taskId 任务ID
     * @param reason 结束/跳过原因
     */
    void finishTask(Long taskId, String reason);
}
