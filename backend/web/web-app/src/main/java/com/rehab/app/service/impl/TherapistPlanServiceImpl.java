package com.rehab.app.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.app.service.TherapistPlanService;
import com.rehab.app.vo.TherapistPlanDetailVO;
import com.rehab.app.vo.TherapistPlanItemVO;
import com.rehab.app.vo.TherapistPlanVO;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.dto.PlanCreateRequest;
import com.rehab.model.dto.PlanItemRequest;
import com.rehab.model.dto.PlanScheduleRequest;
import com.rehab.model.dto.PlanUpdateRequest;
import com.rehab.model.dto.ReasonRequest;
import com.rehab.model.entity.Action;
import com.rehab.model.entity.ActionVersion;
import com.rehab.model.entity.PatientProfile;
import com.rehab.model.entity.PatientTherapist;
import com.rehab.model.entity.PlanItem;
import com.rehab.model.entity.PlanSchedule;
import com.rehab.model.entity.RehabPartDict;
import com.rehab.model.entity.RehabPlan;
import com.rehab.model.entity.TherapistProfile;
import com.rehab.model.enums.BindingStatus;
import com.rehab.model.enums.FrequencyType;
import com.rehab.model.enums.PlanStatus;
import com.rehab.model.enums.VersionStatus;
import com.rehab.model.mapper.account.PatientProfileMapper;
import com.rehab.model.mapper.account.PatientTherapistMapper;
import com.rehab.model.mapper.account.TherapistProfileMapper;
import com.rehab.model.mapper.action.ActionMapper;
import com.rehab.model.mapper.action.ActionVersionMapper;
import com.rehab.model.mapper.plan.PlanItemMapper;
import com.rehab.model.mapper.plan.PlanScheduleMapper;
import com.rehab.model.mapper.plan.RehabPlanMapper;
import com.rehab.model.mapper.system.RehabPartDictMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 治疗师端康复计划服务实现（P6-B3）。
 *
 * <p>对象级权限约定：</p>
 * <ul>
 *   <li>患者维度入口先校验 ACTIVE 绑定（解除/不存在一律 403 OWNERSHIP_DENIED）；</li>
 *   <li>计划维度入口校验 plan.therapistId=当前治疗师（他人计划 404 不泄露存在性），
 *       再校验该患者与当前治疗师绑定仍 ACTIVE（解除后 403）；</li>
 *   <li>仅 DRAFT 可编辑基本信息/动作项/日程与删除；状态变更严格按状态机（违例 409 STATE_CONFLICT）。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TherapistPlanServiceImpl implements TherapistPlanService {

    private final RehabPlanMapper rehabPlanMapper;
    private final PlanItemMapper planItemMapper;
    private final PlanScheduleMapper planScheduleMapper;
    private final ActionVersionMapper actionVersionMapper;
    private final ActionMapper actionMapper;
    private final PatientTherapistMapper patientTherapistMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final TherapistProfileMapper therapistProfileMapper;
    private final RehabPartDictMapper rehabPartDictMapper;

    // ==================== 计划 CRUD ====================

    @Override
    @Transactional
    public TherapistPlanDetailVO createPlan(Long patientId, PlanCreateRequest request) {
        PatientTherapist binding = requireActiveBinding(patientId);
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "结束日期不能早于开始日期");
        }
        validateRehabPartCode(request.getRehabPartCode());
        Long sameName = rehabPlanMapper.selectCount(new LambdaQueryWrapper<RehabPlan>()
                .eq(RehabPlan::getPatientId, patientId)
                .eq(RehabPlan::getName, request.getName()));
        if (sameName != null && sameName > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_RESOURCE, "该患者已存在同名计划，请通过版本复制进行调整");
        }
        RehabPlan plan = new RehabPlan();
        plan.setPatientId(patientId);
        plan.setTherapistId(SecurityUtils.getUserId());
        plan.setBindingId(binding.getBindingId());
        plan.setName(request.getName());
        plan.setRehabPartCode(request.getRehabPartCode());
        plan.setVersionNo(1);
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        // 创建时默认每日训练；执行星期可在草稿期通过日程接口调整
        plan.setFrequencyType(FrequencyType.DAILY.getCode());
        plan.setStatus(PlanStatus.DRAFT.getCode());
        plan.setRemark(request.getRemark());
        rehabPlanMapper.insert(plan);

        int sequenceNo = 1;
        for (PlanCreateRequest.PlanItemPayload payload : request.getItems()) {
            // 创建载荷未带单组时长，时长类范围校验在补设日程/编辑动作项时执行
            validateItemAgainstVersion(payload.getActionVersionId(), payload.getTargetAngle(), null);
            PlanItem item = new PlanItem();
            item.setPlanId(plan.getPlanId());
            item.setActionVersionId(payload.getActionVersionId());
            item.setSequenceNo(sequenceNo++);
            item.setTargetSets(payload.getSets());
            item.setTargetReps(payload.getTargetReps());
            item.setTargetAmplitude(payload.getTargetAngle());
            item.setRestSec(payload.getRestSec());
            planItemMapper.insert(item);
        }
        return getPlanDetail(plan.getPlanId());
    }

    @Override
    public TherapistPlanDetailVO getPlanDetail(Long planId) {
        RehabPlan plan = requireMyPlan(planId);
        requireActiveBinding(plan.getPatientId());
        return buildDetailVO(plan);
    }

    @Override
    @Transactional
    public TherapistPlanDetailVO updatePlan(Long planId, PlanUpdateRequest request) {
        RehabPlan plan = requireMyPlan(planId);
        requireActiveBinding(plan.getPatientId());
        requireDraft(plan);
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(plan.getName())) {
            Long sameName = rehabPlanMapper.selectCount(new LambdaQueryWrapper<RehabPlan>()
                    .eq(RehabPlan::getPatientId, plan.getPatientId())
                    .eq(RehabPlan::getName, request.getName())
                    .ne(RehabPlan::getPlanId, planId));
            if (sameName != null && sameName > 0) {
                throw new BusinessException(ResultCode.DUPLICATE_RESOURCE, "该患者已存在同名计划");
            }
            plan.setName(request.getName());
        }
        if (StringUtils.hasText(request.getRehabPartCode())
                && !request.getRehabPartCode().equals(plan.getRehabPartCode())) {
            validateRehabPartCode(request.getRehabPartCode());
            plan.setRehabPartCode(request.getRehabPartCode());
        }
        if (request.getStartDate() != null) {
            plan.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            plan.setEndDate(request.getEndDate());
        }
        if (plan.getEndDate().isBefore(plan.getStartDate())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "结束日期不能早于开始日期");
        }
        if (request.getRemark() != null) {
            plan.setRemark(request.getRemark());
        }
        rehabPlanMapper.updateById(plan);
        return buildDetailVO(plan);
    }

    @Override
    @Transactional
    public void deleteDraftPlan(Long planId) {
        RehabPlan plan = requireMyPlan(planId);
        requireActiveBinding(plan.getPatientId());
        requireDraft(plan);
        // 草稿无任务/记录/报告引用，物理删除避免 uk_plan_version/uk_plan_item_sequence 残留占位
        planItemMapper.deleteByPlanIdPhysically(planId);
        planScheduleMapper.deleteByPlanIdPhysically(planId);
        rehabPlanMapper.deleteDraftPhysically(planId);
    }

    // ==================== 状态机 ====================

    @Override
    @Transactional
    public TherapistPlanDetailVO publishPlan(Long planId) {
        RehabPlan plan = requireMyPlan(planId);
        requireActiveBinding(plan.getPatientId());
        if (!PlanStatus.DRAFT.getCode().equals(plan.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅草稿计划可发布");
        }
        List<PlanItem> items = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, planId)
                .orderByAsc(PlanItem::getSequenceNo));
        if (items.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "计划至少包含一个动作项，请先添加动作项");
        }
        for (PlanItem item : items) {
            // 发布时以最终动作项重校验（动作版本可能在草稿期间被废弃）
            validateItemAgainstVersion(item.getActionVersionId(), item.getTargetAmplitude(), item.getDurationSec());
        }
        if (!FrequencyType.DAILY.getCode().equals(plan.getFrequencyType())) {
            Long scheduleCount = planScheduleMapper.selectCount(new LambdaQueryWrapper<PlanSchedule>()
                    .eq(PlanSchedule::getPlanId, planId));
            if (scheduleCount == null || scheduleCount == 0) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "每周/自定义频次计划需先设置执行星期");
            }
        }
        // 同名计划链上仅允许一个进行中版本（PUBLISHED/PAUSED），避免多版本重复生成任务
        Long liveOthers = rehabPlanMapper.selectCount(new LambdaQueryWrapper<RehabPlan>()
                .eq(RehabPlan::getPatientId, plan.getPatientId())
                .eq(RehabPlan::getName, plan.getName())
                .ne(RehabPlan::getPlanId, planId)
                .in(RehabPlan::getStatus, PlanStatus.PUBLISHED.getCode(), PlanStatus.PAUSED.getCode()));
        if (liveOthers != null && liveOthers > 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "同名计划已有进行中的版本，请先归档旧版本再发布");
        }
        plan.setStatus(PlanStatus.PUBLISHED.getCode());
        rehabPlanMapper.updateById(plan);
        return buildDetailVO(plan);
    }

    @Override
    @Transactional
    public void pausePlan(Long planId, ReasonRequest request) {
        RehabPlan plan = requireMyPlan(planId);
        requireActiveBinding(plan.getPatientId());
        if (!PlanStatus.PUBLISHED.getCode().equals(plan.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅已发布计划可暂停");
        }
        plan.setStatus(PlanStatus.PAUSED.getCode());
        rehabPlanMapper.updateById(plan);
        // 暂停原因由 Controller 层 @OperationLog(recordParam) 写入操作日志
    }

    @Override
    @Transactional
    public void resumePlan(Long planId) {
        RehabPlan plan = requireMyPlan(planId);
        requireActiveBinding(plan.getPatientId());
        if (!PlanStatus.PAUSED.getCode().equals(plan.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅已暂停计划可恢复");
        }
        plan.setStatus(PlanStatus.PUBLISHED.getCode());
        rehabPlanMapper.updateById(plan);
    }

    @Override
    @Transactional
    public void archivePlan(Long planId, ReasonRequest request) {
        RehabPlan plan = requireMyPlan(planId);
        requireActiveBinding(plan.getPatientId());
        boolean live = PlanStatus.PUBLISHED.getCode().equals(plan.getStatus())
                || PlanStatus.PAUSED.getCode().equals(plan.getStatus());
        if (!live) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅进行中或已暂停的计划可归档（草稿请直接删除）");
        }
        plan.setStatus(PlanStatus.ARCHIVED.getCode());
        rehabPlanMapper.updateById(plan);
    }

    // ==================== 版本 ====================

    @Override
    @Transactional
    public TherapistPlanDetailVO createNewVersion(Long planId) {
        RehabPlan source = requireMyPlan(planId);
        PatientTherapist binding = requireActiveBinding(source.getPatientId());
        RehabPlan maxVersion = rehabPlanMapper.selectOne(new LambdaQueryWrapper<RehabPlan>()
                .eq(RehabPlan::getPatientId, source.getPatientId())
                .eq(RehabPlan::getName, source.getName())
                .orderByDesc(RehabPlan::getVersionNo)
                .last("limit 1"));
        int nextVersionNo = (maxVersion == null ? 0 : maxVersion.getVersionNo()) + 1;

        RehabPlan plan = new RehabPlan();
        plan.setPatientId(source.getPatientId());
        plan.setTherapistId(SecurityUtils.getUserId());
        plan.setBindingId(binding.getBindingId());
        plan.setName(source.getName());
        plan.setRehabPartCode(source.getRehabPartCode());
        plan.setVersionNo(nextVersionNo);
        plan.setStartDate(source.getStartDate());
        plan.setEndDate(source.getEndDate());
        plan.setFrequencyType(source.getFrequencyType());
        plan.setFrequencyValue(source.getFrequencyValue());
        plan.setStatus(PlanStatus.DRAFT.getCode());
        plan.setRemark(source.getRemark());
        rehabPlanMapper.insert(plan);

        for (PlanItem item : planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, planId)
                .orderByAsc(PlanItem::getSequenceNo))) {
            PlanItem copy = new PlanItem();
            copy.setPlanId(plan.getPlanId());
            copy.setActionVersionId(item.getActionVersionId());
            copy.setSequenceNo(item.getSequenceNo());
            copy.setTargetSets(item.getTargetSets());
            copy.setTargetReps(item.getTargetReps());
            copy.setTargetAmplitude(item.getTargetAmplitude());
            copy.setDurationSec(item.getDurationSec());
            copy.setRestSec(item.getRestSec());
            copy.setInstruction(item.getInstruction());
            planItemMapper.insert(copy);
        }
        for (PlanSchedule schedule : planScheduleMapper.selectList(new LambdaQueryWrapper<PlanSchedule>()
                .eq(PlanSchedule::getPlanId, planId))) {
            PlanSchedule copy = new PlanSchedule();
            copy.setPlanId(plan.getPlanId());
            copy.setWeekday(schedule.getWeekday());
            planScheduleMapper.insert(copy);
        }
        return buildDetailVO(plan);
    }

    @Override
    public PageResult<TherapistPlanVO> listVersions(Long planId, Integer page, Integer size) {
        RehabPlan source = requireMyPlan(planId);
        Page<RehabPlan> mpPage = new Page<>(page, size);
        rehabPlanMapper.selectPage(mpPage, new LambdaQueryWrapper<RehabPlan>()
                .eq(RehabPlan::getPatientId, source.getPatientId())
                .eq(RehabPlan::getName, source.getName())
                .orderByDesc(RehabPlan::getVersionNo));
        List<TherapistPlanVO> voList = mpPage.getRecords().stream()
                .map(this::buildPlanVO).collect(Collectors.toList());
        return PageResult.of(mpPage, voList);
    }

    @Override
    public PageResult<TherapistPlanVO> listPatientPlans(Long patientId, String status, Integer page, Integer size) {
        requireActiveBinding(patientId);
        Page<RehabPlan> mpPage = new Page<>(page, size);
        rehabPlanMapper.selectPage(mpPage, new LambdaQueryWrapper<RehabPlan>()
                .eq(RehabPlan::getTherapistId, SecurityUtils.getUserId())
                .eq(RehabPlan::getPatientId, patientId)
                .eq(StringUtils.hasText(status), RehabPlan::getStatus, status)
                .orderByDesc(RehabPlan::getPlanId));
        List<TherapistPlanVO> voList = mpPage.getRecords().stream()
                .map(this::buildPlanVO).collect(Collectors.toList());
        return PageResult.of(mpPage, voList);
    }

    // ==================== 动作项 ====================

    @Override
    public PageResult<TherapistPlanItemVO> listItems(Long planId, Integer page, Integer size) {
        requireMyPlan(planId);
        Page<PlanItem> mpPage = new Page<>(page, size);
        planItemMapper.selectPage(mpPage, new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, planId)
                .orderByAsc(PlanItem::getSequenceNo));
        List<TherapistPlanItemVO> voList = buildItemVOs(mpPage.getRecords());
        return PageResult.of(mpPage, voList);
    }

    @Override
    @Transactional
    public TherapistPlanItemVO addPlanItem(Long planId, PlanItemRequest request) {
        RehabPlan plan = requireMyPlan(planId);
        requireActiveBinding(plan.getPatientId());
        requireDraft(plan);
        validateItemAgainstVersion(request.getActionVersionId(), request.getTargetAmplitude(), request.getDurationSec());
        int sequenceNo = resolveSequenceNo(planId, request.getSequenceNo(), null);
        PlanItem item = new PlanItem();
        item.setPlanId(planId);
        item.setActionVersionId(request.getActionVersionId());
        item.setSequenceNo(sequenceNo);
        item.setTargetSets(request.getTargetSets());
        item.setTargetReps(request.getTargetReps());
        item.setTargetAmplitude(request.getTargetAmplitude());
        item.setDurationSec(request.getDurationSec());
        item.setRestSec(request.getRestSec());
        item.setInstruction(request.getInstruction());
        planItemMapper.insert(item);
        return buildItemVOs(List.of(item)).get(0);
    }

    @Override
    @Transactional
    public TherapistPlanItemVO updatePlanItem(Long planId, Long itemId, PlanItemRequest request) {
        RehabPlan plan = requireMyPlan(planId);
        requireActiveBinding(plan.getPatientId());
        requireDraft(plan);
        PlanItem item = requirePlanItem(planId, itemId);
        validateItemAgainstVersion(request.getActionVersionId(), request.getTargetAmplitude(), request.getDurationSec());
        if (request.getSequenceNo() != null && !request.getSequenceNo().equals(item.getSequenceNo())) {
            resolveSequenceNo(planId, request.getSequenceNo(), itemId);
        }
        item.setActionVersionId(request.getActionVersionId());
        if (request.getSequenceNo() != null) {
            item.setSequenceNo(request.getSequenceNo());
        }
        item.setTargetSets(request.getTargetSets());
        item.setTargetReps(request.getTargetReps());
        item.setTargetAmplitude(request.getTargetAmplitude());
        item.setDurationSec(request.getDurationSec());
        item.setRestSec(request.getRestSec());
        item.setInstruction(request.getInstruction());
        planItemMapper.updateById(item);
        return buildItemVOs(List.of(item)).get(0);
    }

    @Override
    @Transactional
    public void deletePlanItem(Long planId, Long itemId) {
        RehabPlan plan = requireMyPlan(planId);
        requireActiveBinding(plan.getPatientId());
        requireDraft(plan);
        requirePlanItem(planId, itemId);
        planItemMapper.deleteByIdPhysically(itemId);
    }

    // ==================== 日程 ====================

    @Override
    @Transactional
    public TherapistPlanDetailVO updateSchedule(Long planId, PlanScheduleRequest request) {
        RehabPlan plan = requireMyPlan(planId);
        requireActiveBinding(plan.getPatientId());
        requireDraft(plan);
        List<Integer> weekdays = request.getWeekdays() == null ? Collections.emptyList()
                : request.getWeekdays().stream().distinct().sorted().toList();
        if (request.getFrequencyType() != FrequencyType.DAILY && weekdays.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "每周/自定义频次需至少选择一个执行星期");
        }
        planScheduleMapper.deleteByPlanIdPhysically(planId);
        for (Integer weekday : weekdays) {
            PlanSchedule schedule = new PlanSchedule();
            schedule.setPlanId(planId);
            schedule.setWeekday(weekday);
            planScheduleMapper.insert(schedule);
        }
        // frequencyValue 允许清空：用 UpdateWrapper 显式 set，规避 updateById 忽略 null 的行为
        rehabPlanMapper.update(null, new LambdaUpdateWrapper<RehabPlan>()
                .eq(RehabPlan::getPlanId, planId)
                .set(RehabPlan::getFrequencyType, request.getFrequencyType().getCode())
                .set(RehabPlan::getFrequencyValue, request.getFrequencyValue()));
        plan.setFrequencyType(request.getFrequencyType().getCode());
        plan.setFrequencyValue(request.getFrequencyValue());
        return buildDetailVO(plan);
    }

    // ==================== 私有辅助：权限与校验 ====================

    /** 校验当前治疗师与患者的 ACTIVE 绑定，解除/不存在一律 403。 */
    private PatientTherapist requireActiveBinding(Long patientId) {
        Long me = SecurityUtils.getUserId();
        PatientTherapist binding = patientTherapistMapper.selectOne(new LambdaQueryWrapper<PatientTherapist>()
                .eq(PatientTherapist::getTherapistId, me)
                .eq(PatientTherapist::getPatientId, patientId)
                .eq(PatientTherapist::getStatus, BindingStatus.ACTIVE.getCode())
                .last("limit 1"));
        if (binding == null) {
            throw new BusinessException(ResultCode.OWNERSHIP_DENIED, "该患者不在您的负责列表中或绑定已解除");
        }
        return binding;
    }

    /** 计划归属校验：他人计划/不存在统一 404，不泄露存在性。 */
    private RehabPlan requireMyPlan(Long planId) {
        Long me = SecurityUtils.getUserId();
        RehabPlan plan = rehabPlanMapper.selectById(planId);
        if (plan == null || !me.equals(plan.getTherapistId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "计划不存在");
        }
        return plan;
    }

    /** 动作项归属校验。 */
    private PlanItem requirePlanItem(Long planId, Long itemId) {
        PlanItem item = planItemMapper.selectById(itemId);
        if (item == null || !planId.equals(item.getPlanId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "计划动作项不存在");
        }
        return item;
    }

    private void requireDraft(RehabPlan plan) {
        if (!PlanStatus.DRAFT.getCode().equals(plan.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅草稿计划可编辑或删除，已发布计划请通过版本复制调整");
        }
    }

    private void validateRehabPartCode(String rehabPartCode) {
        if (!StringUtils.hasText(rehabPartCode)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "康复部位不能为空");
        }
        RehabPartDict dict = rehabPartDictMapper.selectById(rehabPartCode);
        if (dict == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "康复部位编码不存在：" + rehabPartCode);
        }
    }

    /**
     * 动作项参数与动作版本允许范围校验（422 PLAN_PARAMETER_INVALID）。
     *
     * <p>首版量化口径（来源于 action_version.motion_signature，字段缺省时跳过该项校验）：</p>
     * <ul>
     *   <li>动作版本必须存在且为 PUBLISHED；</li>
     *   <li>目标幅度 ≥ minRom，且 ≤ peakAngleRange[1]；</li>
     *   <li>单组时长 ≥ durationRangeSec[1]（至少覆盖一个完整标准周期）。</li>
     * </ul>
     */
    private void validateItemAgainstVersion(Long actionVersionId, BigDecimal targetAmplitude, Integer durationSec) {
        ActionVersion version = actionVersionMapper.selectById(actionVersionId);
        if (version == null || !VersionStatus.PUBLISHED.getCode().equals(version.getStatus())) {
            throw new BusinessException(ResultCode.PLAN_PARAMETER_INVALID, "动作版本不存在或未发布，无法加入计划");
        }
        JSONObject signature = parseJsonObject(version.getMotionSignature());
        if (targetAmplitude != null) {
            Integer minRom = signature.getInt("minRom");
            if (minRom != null && targetAmplitude.doubleValue() < minRom) {
                throw new BusinessException(ResultCode.PLAN_PARAMETER_INVALID,
                        "目标幅度低于动作最小活动度 " + minRom + " 度");
            }
            JSONArray peakRange = signature.getJSONArray("peakAngleRange");
            if (peakRange != null && peakRange.size() >= 2 && peakRange.get(1) instanceof Number maxAngle) {
                if (targetAmplitude.doubleValue() > maxAngle.doubleValue()) {
                    throw new BusinessException(ResultCode.PLAN_PARAMETER_INVALID,
                            "目标幅度超出动作允许峰值 " + maxAngle.intValue() + " 度");
                }
            }
        }
        if (durationSec != null) {
            JSONArray durationRange = signature.getJSONArray("durationRangeSec");
            if (durationRange != null && durationRange.size() >= 2 && durationRange.get(1) instanceof Number maxCycle) {
                int minDurationSec = (int) Math.ceil(maxCycle.doubleValue());
                if (durationSec < minDurationSec) {
                    throw new BusinessException(ResultCode.PLAN_PARAMETER_INVALID,
                            "单组时长需不少于单周期最大时长 " + minDurationSec + " 秒");
                }
            }
        }
    }

    /** 防御性解析 JSON（motion_signature 历史值可能为 "[]" 或空）。 */
    private JSONObject parseJsonObject(String text) {
        if (!StringUtils.hasText(text)) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(text);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    /** 解析顺序号：缺省取同计划最大值 +1；显式提供时查重（可排除自身）。 */
    private int resolveSequenceNo(Long planId, Integer requested, Long excludeItemId) {
        if (requested != null) {
            Long dup = planItemMapper.selectCount(new LambdaQueryWrapper<PlanItem>()
                    .eq(PlanItem::getPlanId, planId)
                    .eq(PlanItem::getSequenceNo, requested)
                    .ne(excludeItemId != null, PlanItem::getPlanItemId, excludeItemId));
            if (dup != null && dup > 0) {
                throw new BusinessException(ResultCode.DUPLICATE_RESOURCE, "顺序号 " + requested + " 已存在");
            }
            return requested;
        }
        PlanItem max = planItemMapper.selectOne(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, planId)
                .orderByDesc(PlanItem::getSequenceNo)
                .last("limit 1"));
        return (max == null ? 0 : max.getSequenceNo()) + 1;
    }

    // ==================== VO 组装 ====================

    private TherapistPlanDetailVO buildDetailVO(RehabPlan plan) {
        TherapistPlanDetailVO vo = new TherapistPlanDetailVO();
        copyPlanFields(plan, vo);
        List<PlanItem> items = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, plan.getPlanId())
                .orderByAsc(PlanItem::getSequenceNo));
        vo.setItems(buildItemVOs(items));
        vo.setWeekdays(planScheduleMapper.selectList(new LambdaQueryWrapper<PlanSchedule>()
                        .eq(PlanSchedule::getPlanId, plan.getPlanId()))
                .stream().map(PlanSchedule::getWeekday).sorted().toList());
        return vo;
    }

    private TherapistPlanVO buildPlanVO(RehabPlan plan) {
        TherapistPlanVO vo = new TherapistPlanVO();
        copyPlanFields(plan, vo);
        return vo;
    }

    private void copyPlanFields(RehabPlan plan, TherapistPlanVO vo) {
        vo.setPlanId(plan.getPlanId());
        vo.setPatientId(plan.getPatientId());
        vo.setName(plan.getName());
        vo.setRehabPartCode(plan.getRehabPartCode());
        vo.setVersionNo(plan.getVersionNo());
        vo.setStartDate(plan.getStartDate());
        vo.setEndDate(plan.getEndDate());
        vo.setFrequencyType(plan.getFrequencyType());
        vo.setFrequencyValue(plan.getFrequencyValue());
        vo.setStatus(plan.getStatus());
        vo.setStatusName(PlanStatus.fromCode(plan.getStatus()).getName());
        vo.setRemark(plan.getRemark());
        vo.setCreatedAt(plan.getCreatedAt());
        vo.setUpdatedAt(plan.getUpdatedAt());
        vo.setItemCount(planItemMapper.selectCount(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, plan.getPlanId())));
        if (StringUtils.hasText(plan.getRehabPartCode())) {
            RehabPartDict dict = rehabPartDictMapper.selectById(plan.getRehabPartCode());
            vo.setRehabPartName(dict != null ? dict.getName() : null);
        }
        vo.setPatientName(queryPatientName(plan.getPatientId()));
    }

    private String queryPatientName(Long patientUserId) {
        PatientProfile profile = patientProfileMapper.selectOne(new LambdaQueryWrapper<PatientProfile>()
                .eq(PatientProfile::getUserId, patientUserId)
                .last("limit 1"));
        return profile != null ? profile.getRealName() : null;
    }

    /** 批量组装动作项 VO（版本/动作两级批量查询，避免 N+1）。 */
    private List<TherapistPlanItemVO> buildItemVOs(List<PlanItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> versionIds = items.stream().map(PlanItem::getActionVersionId).distinct().toList();
        Map<Long, ActionVersion> versionMap = actionVersionMapper.selectBatchIds(versionIds).stream()
                .collect(Collectors.toMap(ActionVersion::getActionVersionId, Function.identity()));
        List<Long> actionIds = versionMap.values().stream().map(ActionVersion::getActionId).distinct().toList();
        Map<Long, Action> actionMap = actionIds.isEmpty() ? Map.of()
                : actionMapper.selectBatchIds(actionIds).stream()
                        .collect(Collectors.toMap(Action::getActionId, Function.identity()));
        return items.stream().map(item -> {
            TherapistPlanItemVO vo = new TherapistPlanItemVO();
            vo.setPlanItemId(item.getPlanItemId());
            vo.setPlanId(item.getPlanId());
            vo.setActionVersionId(item.getActionVersionId());
            vo.setSequenceNo(item.getSequenceNo());
            vo.setTargetSets(item.getTargetSets());
            vo.setTargetReps(item.getTargetReps());
            vo.setTargetAmplitude(item.getTargetAmplitude());
            vo.setDurationSec(item.getDurationSec());
            vo.setRestSec(item.getRestSec());
            vo.setInstruction(item.getInstruction());
            ActionVersion version = versionMap.get(item.getActionVersionId());
            if (version != null) {
                vo.setActionId(version.getActionId());
                vo.setVersionNo(version.getVersionNo());
                Action action = actionMap.get(version.getActionId());
                if (action != null) {
                    vo.setActionName(action.getName());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
