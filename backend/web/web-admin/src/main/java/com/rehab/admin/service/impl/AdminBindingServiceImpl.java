package com.rehab.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rehab.admin.service.AdminBindingService;
import com.rehab.model.mapper.account.PatientProfileMapper;
import com.rehab.model.mapper.account.PatientTherapistMapper;
import com.rehab.model.mapper.account.TherapistProfileMapper;
import com.rehab.model.mapper.account.UserAccountMapper;
import com.rehab.common.constant.Constants;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.dto.BindingCreateRequest;
import com.rehab.model.dto.BindingUpdateRequest;
import com.rehab.model.dto.ReasonRequest;
import com.rehab.model.entity.PatientProfile;
import com.rehab.model.entity.PatientTherapist;
import com.rehab.model.entity.TherapistProfile;
import com.rehab.model.entity.UserAccount;
import com.rehab.model.enums.BindingStatus;
import com.rehab.model.enums.RelationType;
import com.rehab.model.enums.RoleCode;
import com.rehab.admin.vo.BindingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端 - 医患绑定关系服务实现。
 *
 * <p>主实体 patient_therapist 的通用 CRUD 走 MyBatis-Plus {@link ServiceImpl}；
 * 双方账号与姓名解析属于协作表查询，直接使用其 BaseMapper。</p>
 */
@Service
@RequiredArgsConstructor
public class AdminBindingServiceImpl extends ServiceImpl<PatientTherapistMapper, PatientTherapist>
        implements AdminBindingService {

    // 协作 Mapper：主实体 patient_therapist 由 ServiceImpl.baseMapper 承载，无需重复注入
    private final UserAccountMapper userAccountMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final TherapistProfileMapper therapistProfileMapper;

    @Override
    public PageResult<BindingVO> pageBindings(Integer page, Integer size,
                                              Long patientId, Long therapistId, String status) {
        LambdaQueryWrapper<PatientTherapist> wrapper = new LambdaQueryWrapper<PatientTherapist>()
                .eq(patientId != null, PatientTherapist::getPatientId, patientId)
                .eq(therapistId != null, PatientTherapist::getTherapistId, therapistId)
                .eq(StringUtils.hasText(status), PatientTherapist::getStatus, status)
                .orderByDesc(PatientTherapist::getBindingId);

        Page<PatientTherapist> result = this.page(new Page<>(page, size), wrapper);
        if (result.getRecords().isEmpty()) {
            return PageResult.of(result, List.of());
        }
        // 批量解析双方姓名，避免 N+1 查询
        Map<Long, String> patientNames = loadPatientNames(result.getRecords());
        Map<Long, String> therapistNames = loadTherapistNames(result.getRecords());
        List<BindingVO> voList = result.getRecords().stream()
                .map(b -> toVO(b, patientNames, therapistNames))
                .toList();
        return PageResult.of(result, voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBinding(BindingCreateRequest request) {
        UserAccount patient = requireEnabledUser(request.getPatientId(), RoleCode.PATIENT, "患者");
        UserAccount therapist = requireEnabledUser(request.getTherapistId(), RoleCode.THERAPIST, "治疗师");

        LocalDateTime startAt = request.getStartAt() != null ? request.getStartAt() : LocalDateTime.now();
        LocalDateTime endAt = request.getEndAt();
        if (endAt != null && !endAt.isAfter(startAt)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "绑定结束时间必须晚于开始时间");
        }

        // 同一对患者-治疗师已存在有效绑定，不允许重复建立
        if (this.exists(new LambdaQueryWrapper<PatientTherapist>()
                .eq(PatientTherapist::getPatientId, patient.getUserId())
                .eq(PatientTherapist::getTherapistId, therapist.getUserId())
                .eq(PatientTherapist::getStatus, BindingStatus.ACTIVE.getCode()))) {
            throw new BusinessException(ResultCode.DUPLICATE_RESOURCE, "该患者与治疗师已存在有效绑定关系");
        }

        // 主责绑定：同一患者在重叠有效期内只能有一个 ACTIVE 的 PRIMARY
        if (request.getRelationType() == RelationType.PRIMARY) {
            List<PatientTherapist> conflicts = this.list(
                    new LambdaQueryWrapper<PatientTherapist>()
                            .eq(PatientTherapist::getPatientId, patient.getUserId())
                            .eq(PatientTherapist::getRelationType, RelationType.PRIMARY.getCode())
                            .eq(PatientTherapist::getStatus, BindingStatus.ACTIVE.getCode()));
            for (PatientTherapist existing : conflicts) {
                if (periodOverlap(existing.getStartAt(), existing.getEndAt(), startAt, endAt)) {
                    throw new BusinessException(ResultCode.BINDING_CONFLICT,
                            "该患者在同一有效期内已存在主责治疗师，不能重复指定");
                }
            }
        }

        PatientTherapist binding = new PatientTherapist();
        binding.setPatientId(patient.getUserId());
        binding.setTherapistId(therapist.getUserId());
        binding.setRelationType(request.getRelationType().getCode());
        binding.setStatus(BindingStatus.ACTIVE.getCode());
        binding.setStartAt(startAt);
        binding.setEndAt(endAt);
        binding.setCreatedBy(SecurityUtils.getUserId());
        this.save(binding);
        return binding.getBindingId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBinding(Long bindingId, BindingUpdateRequest request) {
        PatientTherapist binding = requireActiveBinding(bindingId);

        LocalDateTime startAt = request.getStartAt() != null ? request.getStartAt() : binding.getStartAt();
        LocalDateTime endAt = request.getEndAt() != null ? request.getEndAt() : binding.getEndAt();
        if (endAt != null && !endAt.isAfter(startAt)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "绑定结束时间必须晚于开始时间");
        }
        RelationType relationType = request.getRelationType() != null
                ? request.getRelationType()
                : RelationType.fromCode(binding.getRelationType());

        // 变更为主责时需重新做唯一性/有效期冲突校验（排除自身）
        if (relationType == RelationType.PRIMARY) {
            List<PatientTherapist> conflicts = this.list(
                    new LambdaQueryWrapper<PatientTherapist>()
                            .eq(PatientTherapist::getPatientId, binding.getPatientId())
                            .eq(PatientTherapist::getRelationType, RelationType.PRIMARY.getCode())
                            .eq(PatientTherapist::getStatus, BindingStatus.ACTIVE.getCode())
                            .ne(PatientTherapist::getBindingId, bindingId));
            for (PatientTherapist existing : conflicts) {
                if (periodOverlap(existing.getStartAt(), existing.getEndAt(), startAt, endAt)) {
                    throw new BusinessException(ResultCode.BINDING_CONFLICT,
                            "该患者在同一有效期内已存在主责治疗师，不能重复指定");
                }
            }
        }

        binding.setRelationType(relationType.getCode());
        binding.setStartAt(startAt);
        binding.setEndAt(endAt);
        updateById(binding);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long bindingId, ReasonRequest reason) {
        PatientTherapist binding = requireActiveBinding(bindingId);
        // 历史业务数据（计划/咨询/训练记录）保留只读：仅状态流转，不物理删除
        binding.setStatus(BindingStatus.ENDED.getCode());
        binding.setEndAt(LocalDateTime.now());
        updateById(binding);
    }

    // ==================== 私有辅助方法 ====================

    private UserAccount requireEnabledUser(Long userId, RoleCode expectedRole, String roleLabel) {
        if (userId == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, roleLabel + "ID不能为空");
        }
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, roleLabel + "账号不存在：" + userId);
        }
        if (!expectedRole.getCode().equals(user.getRoleCode())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "账号 " + userId + " 不是" + roleLabel + "角色");
        }
        if (!Constants.ENABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, roleLabel + "账号已禁用，无法建立绑定");
        }
        return user;
    }

    private PatientTherapist requireActiveBinding(Long bindingId) {
        PatientTherapist binding = getById(bindingId);
        if (binding == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "绑定关系不存在：" + bindingId);
        }
        if (!BindingStatus.ACTIVE.getCode().equals(binding.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "绑定已结束，不能执行该操作");
        }
        return binding;
    }

    /**
     * 两个半开区间是否重叠：[s1, e1) 与 [s2, e2)，end 为 null 表示长期有效（+∞）。
     */
    private boolean periodOverlap(LocalDateTime s1, LocalDateTime e1,
                                  LocalDateTime s2, LocalDateTime e2) {
        if (e1 == null || e2 == null) {
            return true;
        }
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    private Map<Long, String> loadPatientNames(List<PatientTherapist> bindings) {
        List<Long> ids = bindings.stream().map(PatientTherapist::getPatientId).distinct().toList();
        Map<Long, String> map = new HashMap<>();
        patientProfileMapper.selectList(new LambdaQueryWrapper<PatientProfile>()
                        .in(PatientProfile::getUserId, ids))
                .forEach(p -> map.put(p.getUserId(), p.getRealName()));
        return map;
    }

    private Map<Long, String> loadTherapistNames(List<PatientTherapist> bindings) {
        List<Long> ids = bindings.stream().map(PatientTherapist::getTherapistId).distinct().toList();
        Map<Long, String> map = new HashMap<>();
        therapistProfileMapper.selectList(new LambdaQueryWrapper<TherapistProfile>()
                        .in(TherapistProfile::getUserId, ids))
                .forEach(t -> map.put(t.getUserId(), t.getRealName()));
        return map;
    }

    private BindingVO toVO(PatientTherapist b, Map<Long, String> patientNames,
                           Map<Long, String> therapistNames) {
        return BindingVO.builder()
                .bindingId(b.getBindingId())
                .patientId(b.getPatientId())
                .patientName(patientNames.getOrDefault(b.getPatientId(), "用户" + b.getPatientId()))
                .therapistId(b.getTherapistId())
                .therapistName(therapistNames.getOrDefault(b.getTherapistId(), "用户" + b.getTherapistId()))
                .relationType(b.getRelationType())
                .relationTypeName(relationTypeName(b.getRelationType()))
                .status(b.getStatus())
                .statusName(bindingStatusName(b.getStatus()))
                .startAt(b.getStartAt())
                .endAt(b.getEndAt())
                .createdBy(b.getCreatedBy())
                .createdAt(b.getCreatedAt())
                .build();
    }

    private String relationTypeName(String code) {
        for (RelationType value : RelationType.values()) {
            if (value.getCode().equals(code)) {
                return value.getName();
            }
        }
        return code;
    }

    private String bindingStatusName(String code) {
        for (BindingStatus value : BindingStatus.values()) {
            if (value.getCode().equals(code)) {
                return value.getName();
            }
        }
        return code;
    }
}
