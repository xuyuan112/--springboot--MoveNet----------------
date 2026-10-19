package com.rehab.app.service;

import com.rehab.app.vo.TherapistPlanDetailVO;
import com.rehab.app.vo.TherapistPlanItemVO;
import com.rehab.app.vo.TherapistPlanVO;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.PlanCreateRequest;
import com.rehab.model.dto.PlanItemRequest;
import com.rehab.model.dto.PlanScheduleRequest;
import com.rehab.model.dto.PlanUpdateRequest;
import com.rehab.model.dto.ReasonRequest;

/**
 * 治疗师端康复计划服务（P6-B3，接口文档 5.2）。
 *
 * <p>状态机：{@code DRAFT → PUBLISHED → PAUSED/ARCHIVED}（COMPLETED 终态预留）；
 * 仅 DRAFT 可编辑/删除；发布按 P2 调度生成任务；调整走版本复制，旧版本任务与记录保留不迁移；
 * 所有写操作幂等并由 Controller 层写 operation_log。</p>
 */
public interface TherapistPlanService {

    /** 创建 DRAFT 计划（校验患者 ACTIVE 绑定），含动作项。 */
    TherapistPlanDetailVO createPlan(Long patientId, PlanCreateRequest request);

    /** 计划详情：基本信息 + 动作项全量 + 执行星期。 */
    TherapistPlanDetailVO getPlanDetail(Long planId);

    /** 修改草稿计划基本信息（仅 DRAFT；已发布 409）。 */
    TherapistPlanDetailVO updatePlan(Long planId, PlanUpdateRequest request);

    /** 删除草稿计划（仅 DRAFT；发布后请用归档）。 */
    void deleteDraftPlan(Long planId);

    /** 发布：DRAFT→PUBLISHED；校验动作版本 PUBLISHED、参数范围、日期与执行星期。 */
    TherapistPlanDetailVO publishPlan(Long planId);

    /** 暂停：PUBLISHED→PAUSED，需填写原因。 */
    void pausePlan(Long planId, ReasonRequest request);

    /** 恢复：PAUSED→PUBLISHED。 */
    void resumePlan(Long planId);

    /** 归档（结束/作废）：PUBLISHED/PAUSED→ARCHIVED，停止生成新任务。 */
    void archivePlan(Long planId, ReasonRequest request);

    /** 版本复制：从当前计划复制创建新 DRAFT 版本（版本号 +1，绑定校验）。 */
    TherapistPlanDetailVO createNewVersion(Long planId);

    /** 分页查询同名计划版本链（版本号倒序）。 */
    PageResult<TherapistPlanVO> listVersions(Long planId, Integer page, Integer size);

    /** 分页查询患者的计划（本人创建，planId 倒序，可按状态过滤）。 */
    PageResult<TherapistPlanVO> listPatientPlans(Long patientId, String status, Integer page, Integer size);

    /** 分页查询动作项。 */
    PageResult<TherapistPlanItemVO> listItems(Long planId, Integer page, Integer size);

    /** 新增草稿动作项（校验动作版本 PUBLISHED 与参数范围）。 */
    TherapistPlanItemVO addPlanItem(Long planId, PlanItemRequest request);

    /** 修改草稿动作项。 */
    TherapistPlanItemVO updatePlanItem(Long planId, Long itemId, PlanItemRequest request);

    /** 删除草稿动作项。 */
    void deletePlanItem(Long planId, Long itemId);

    /** 设置执行日程（频次 + 执行星期，仅 DRAFT）。 */
    TherapistPlanDetailVO updateSchedule(Long planId, PlanScheduleRequest request);
}
