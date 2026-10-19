package com.rehab.admin.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rehab.admin.service.AdminUserService;
import com.rehab.model.mapper.system.OperationLogMapper;
import com.rehab.model.mapper.account.PatientProfileMapper;
import com.rehab.model.mapper.account.TherapistProfileMapper;
import com.rehab.model.mapper.account.UserAccountMapper;
import com.rehab.common.constant.Constants;
import com.rehab.common.constant.RedisKeyConstants;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.model.dto.AdminUserCreateRequest;
import com.rehab.model.dto.AdminUserUpdateRequest;
import com.rehab.model.dto.ReasonRequest;
import com.rehab.model.entity.OperationLog;
import com.rehab.model.entity.PatientProfile;
import com.rehab.model.entity.TherapistProfile;
import com.rehab.model.entity.UserAccount;
import com.rehab.model.enums.OperationStatus;
import com.rehab.model.enums.RoleCode;
import com.rehab.admin.vo.AdminUserCreateVO;
import com.rehab.admin.vo.AdminUserDetailVO;
import com.rehab.admin.vo.AdminUserVO;
import com.rehab.admin.vo.LoginLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 管理端 - 账号服务实现。
 *
 * <p>主实体 user_account 的通用 CRUD 走 MyBatis-Plus {@link ServiceImpl}；
 * 患者档案/治疗师档案/审计日志为协作表，直接使用其 BaseMapper。</p>
 */
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount>
        implements AdminUserService {

    /** 平台初始密码（创建账号 / 管理员重置密码时使用；通过线下渠道告知，接口不回显明文） */
    private static final String DEFAULT_INITIAL_PASSWORD = "Rehab@123456";

    // 协作 Mapper：主实体 user_account 由 ServiceImpl.baseMapper 承载，无需重复注入
    private final PatientProfileMapper patientProfileMapper;
    private final TherapistProfileMapper therapistProfileMapper;
    private final OperationLogMapper operationLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResult<AdminUserVO> pageUsers(Integer page, Integer size,
                                             String roleCode, Integer status, String keyword) {
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<UserAccount>()
                .eq(StringUtils.hasText(roleCode), UserAccount::getRoleCode, roleCode)
                .eq(status != null, UserAccount::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(UserAccount::getUsername, keyword)
                        .or().like(UserAccount::getPhone, keyword)
                        .or().inSql(UserAccount::getUserId,
                                "SELECT user_id FROM patient_profile WHERE deleted = 0 AND real_name LIKE '%"
                                        + escapeSqlLike(keyword) + "%'")
                        .or().inSql(UserAccount::getUserId,
                                "SELECT user_id FROM therapist_profile WHERE deleted = 0 AND real_name LIKE '%"
                                        + escapeSqlLike(keyword) + "%'"))
                .orderByDesc(UserAccount::getUserId);

        Page<UserAccount> result = this.page(new Page<>(page, size), wrapper);
        List<AdminUserVO> voList = result.getRecords().stream().map(this::toListVO).toList();
        return PageResult.of(result, voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserCreateVO createUser(AdminUserCreateRequest request) {
        // 仅允许创建患者/治疗师账号，ADMIN 为系统内置角色
        if (request.getRoleCode() != RoleCode.PATIENT && request.getRoleCode() != RoleCode.THERAPIST) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "仅允许创建患者或治疗师账号");
        }
        if (this.exists(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUsername, request.getUsername()))) {
            throw new BusinessException(ResultCode.DUPLICATE_RESOURCE, "登录名已存在：" + request.getUsername());
        }
        if (StringUtils.hasText(request.getPhone()) && this.exists(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getPhone, request.getPhone()))) {
            throw new BusinessException(ResultCode.DUPLICATE_RESOURCE, "手机号已被其他账号使用");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_INITIAL_PASSWORD));
        user.setRoleCode(request.getRoleCode().getCode());
        user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        user.setStatus(Constants.ENABLED);
        this.save(user);

        if (request.getRoleCode() == RoleCode.PATIENT) {
            createPatientProfile(user.getUserId(), request);
        } else {
            createTherapistProfile(user.getUserId(), request);
        }

        return AdminUserCreateVO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .roleCode(user.getRoleCode())
                .initialPasswordDelivered(Boolean.FALSE)
                .mustChangePassword(Boolean.TRUE)
                .build();
    }

    @Override
    public AdminUserDetailVO detail(Long userId) {
        UserAccount user = requireUser(userId);
        AdminUserDetailVO.AdminUserDetailVOBuilder b = AdminUserDetailVO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .roleCode(user.getRoleCode())
                .roleName(roleName(user.getRoleCode()))
                .phone(user.getPhone())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt());

        if (RoleCode.PATIENT.getCode().equals(user.getRoleCode())) {
            PatientProfile p = findPatientProfile(userId);
            if (p != null) {
                b.realName(p.getRealName())
                        .gender(p.getGender())
                        .birthDate(p.getBirthDate())
                        .heightCm(p.getHeightCm())
                        .weightKg(p.getWeightKg())
                        .medicalHistory(p.getMedicalHistory())
                        .allergyInfo(p.getAllergyInfo())
                        .contraindication(p.getContraindication());
            }
        } else if (RoleCode.THERAPIST.getCode().equals(user.getRoleCode())) {
            TherapistProfile t = findTherapistProfile(userId);
            if (t != null) {
                b.realName(t.getRealName())
                        .title(t.getTitle())
                        .specialty(t.getSpecialty())
                        .licenseNo(t.getLicenseNo())
                        .introduction(t.getIntroduction());
            }
        } else {
            b.realName(user.getUsername());
        }
        return b.build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, AdminUserUpdateRequest request) {
        UserAccount user = requireManagedUser(userId);

        if (StringUtils.hasText(request.getPhone())) {
            String phone = request.getPhone().trim();
            if (this.exists(new LambdaQueryWrapper<UserAccount>()
                    .eq(UserAccount::getPhone, phone)
                    .ne(UserAccount::getUserId, userId))) {
                throw new BusinessException(ResultCode.DUPLICATE_RESOURCE, "手机号已被其他账号使用");
            }
            user.setPhone(phone);
        }
        updateById(user);

        if (RoleCode.PATIENT.getCode().equals(user.getRoleCode())) {
            PatientProfile p = findPatientProfile(userId);
            if (p == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "患者档案不存在");
            }
            if (StringUtils.hasText(request.getRealName())) {
                p.setRealName(request.getRealName().trim());
            }
            if (request.getGender() != null) {
                p.setGender(request.getGender().getCode());
            }
            if (request.getBirthDate() != null) {
                p.setBirthDate(request.getBirthDate());
            }
            if (request.getHeightCm() != null) {
                p.setHeightCm(request.getHeightCm());
            }
            if (request.getWeightKg() != null) {
                p.setWeightKg(request.getWeightKg());
            }
            if (request.getMedicalHistory() != null) {
                p.setMedicalHistory(request.getMedicalHistory());
            }
            if (request.getAllergyInfo() != null) {
                p.setAllergyInfo(request.getAllergyInfo());
            }
            if (request.getContraindication() != null) {
                p.setContraindication(request.getContraindication());
            }
            patientProfileMapper.updateById(p);
        } else if (RoleCode.THERAPIST.getCode().equals(user.getRoleCode())) {
            TherapistProfile t = findTherapistProfile(userId);
            if (t == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "治疗师档案不存在");
            }
            if (StringUtils.hasText(request.getRealName())) {
                t.setRealName(request.getRealName().trim());
            }
            if (StringUtils.hasText(request.getTitle())) {
                t.setTitle(request.getTitle().trim());
            }
            if (StringUtils.hasText(request.getSpecialty())) {
                t.setSpecialty(request.getSpecialty().trim());
            }
            if (request.getLicenseNo() != null) {
                if (StringUtils.hasText(request.getLicenseNo())
                        && therapistProfileMapper.exists(new LambdaQueryWrapper<TherapistProfile>()
                                .eq(TherapistProfile::getLicenseNo, request.getLicenseNo().trim())
                                .ne(TherapistProfile::getUserId, userId))) {
                    throw new BusinessException(ResultCode.DUPLICATE_RESOURCE, "资质编号已被使用");
                }
                t.setLicenseNo(StringUtils.hasText(request.getLicenseNo()) ? request.getLicenseNo().trim() : null);
            }
            if (request.getIntroduction() != null) {
                t.setIntroduction(request.getIntroduction());
            }
            therapistProfileMapper.updateById(t);
        }
    }

    @Override
    public void enable(Long userId) {
        UserAccount user = requireManagedUser(userId);
        if (Constants.ENABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "账号已是启用状态");
        }
        user.setStatus(Constants.ENABLED);
        updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long userId, ReasonRequest reason) {
        UserAccount user = requireManagedUser(userId);
        if (!Constants.ENABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "账号已是禁用状态");
        }
        user.setStatus(Constants.DISABLED);
        updateById(user);
        // 删除会话使令牌立即失效；同时清理登录失败计数与锁定键
        invalidateSessions(user);
    }

    @Override
    public void resetPassword(Long userId) {
        UserAccount user = requireManagedUser(userId);
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_INITIAL_PASSWORD));
        updateById(user);
        invalidateSessions(user);
    }

    @Override
    public PageResult<LoginLogVO> pageLoginLogs(Long userId, Integer page, Integer size, Boolean success) {
        requireUser(userId);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getModule, "AUTH")
                .eq(OperationLog::getUserId, userId)
                .eq(success != null, OperationLog::getStatus,
                        Boolean.TRUE.equals(success)
                                ? OperationStatus.SUCCESS.getCode()
                                : OperationStatus.FAIL.getCode())
                .orderByDesc(OperationLog::getCreatedAt)
                .orderByDesc(OperationLog::getLogId);

        Page<OperationLog> result = operationLogMapper.selectPage(new Page<>(page, size), wrapper);
        List<LoginLogVO> voList = result.getRecords().stream()
                .map(log -> LoginLogVO.builder()
                        .logId(log.getLogId())
                        .userId(log.getUserId())
                        .action(log.getAction())
                        .status(log.getStatus())
                        .ip(log.getIp())
                        .deviceId(log.getDeviceId())
                        .message(extractMessage(log.getDetail()))
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
        return PageResult.of(result, voList);
    }

    // ==================== 私有辅助方法 ====================

    private void createPatientProfile(Long userId, AdminUserCreateRequest req) {
        // 数据库列为 NOT NULL 的字段在服务层显式校验，避免依赖数据库异常
        if (req.getGender() == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "创建患者账号时性别不能为空");
        }
        if (req.getBirthDate() == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "创建患者账号时出生日期不能为空");
        }
        PatientProfile p = new PatientProfile();
        p.setUserId(userId);
        p.setRealName(req.getRealName().trim());
        p.setGender(req.getGender().getCode());
        p.setBirthDate(req.getBirthDate());
        p.setHeightCm(req.getHeightCm());
        p.setWeightKg(req.getWeightKg());
        p.setMedicalHistory(req.getMedicalHistory());
        p.setAllergyInfo(req.getAllergyInfo());
        p.setContraindication(req.getContraindication());
        patientProfileMapper.insert(p);
    }

    private void createTherapistProfile(Long userId, AdminUserCreateRequest req) {
        if (!StringUtils.hasText(req.getTitle())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "创建治疗师账号时职称不能为空");
        }
        if (!StringUtils.hasText(req.getSpecialty())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "创建治疗师账号时专长不能为空");
        }
        if (StringUtils.hasText(req.getLicenseNo()) && therapistProfileMapper.exists(
                new LambdaQueryWrapper<TherapistProfile>()
                        .eq(TherapistProfile::getLicenseNo, req.getLicenseNo().trim()))) {
            throw new BusinessException(ResultCode.DUPLICATE_RESOURCE, "资质编号已被使用");
        }
        TherapistProfile t = new TherapistProfile();
        t.setUserId(userId);
        t.setRealName(req.getRealName().trim());
        t.setTitle(req.getTitle().trim());
        t.setSpecialty(req.getSpecialty().trim());
        t.setLicenseNo(StringUtils.hasText(req.getLicenseNo()) ? req.getLicenseNo().trim() : null);
        t.setIntroduction(req.getIntroduction());
        therapistProfileMapper.insert(t);
    }

    private UserAccount requireUser(Long userId) {
        UserAccount user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "账号不存在：" + userId);
        }
        return user;
    }

    /**
     * 账号启停/改密仅针对患者与治疗师；ADMIN 为系统内置账号，不允许经此接口操作。
     */
    private UserAccount requireManagedUser(Long userId) {
        UserAccount user = requireUser(userId);
        if (RoleCode.ADMIN.getCode().equals(user.getRoleCode())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "管理员账号不支持此操作");
        }
        return user;
    }

    private PatientProfile findPatientProfile(Long userId) {
        return patientProfileMapper.selectOne(new LambdaQueryWrapper<PatientProfile>()
                .eq(PatientProfile::getUserId, userId)
                .last("limit 1"));
    }

    private TherapistProfile findTherapistProfile(Long userId) {
        return therapistProfileMapper.selectOne(new LambdaQueryWrapper<TherapistProfile>()
                .eq(TherapistProfile::getUserId, userId)
                .last("limit 1"));
    }

    private AdminUserVO toListVO(UserAccount user) {
        return AdminUserVO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .roleCode(user.getRoleCode())
                .roleName(roleName(user.getRoleCode()))
                .phone(maskPhone(user.getPhone()))
                .realName(resolveDisplayName(user))
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String resolveDisplayName(UserAccount user) {
        if (RoleCode.PATIENT.getCode().equals(user.getRoleCode())) {
            PatientProfile p = findPatientProfile(user.getUserId());
            if (p != null && StringUtils.hasText(p.getRealName())) {
                return p.getRealName();
            }
        } else if (RoleCode.THERAPIST.getCode().equals(user.getRoleCode())) {
            TherapistProfile t = findTherapistProfile(user.getUserId());
            if (t != null && StringUtils.hasText(t.getRealName())) {
                return t.getRealName();
            }
        }
        return user.getUsername();
    }

    private void invalidateSessions(UserAccount user) {
        stringRedisTemplate.delete(RedisKeyConstants.JWT_SESSION + user.getUserId());
        stringRedisTemplate.delete(RedisKeyConstants.LOGIN_FAIL_COUNT + user.getUsername());
        stringRedisTemplate.delete(RedisKeyConstants.LOGIN_LOCK + user.getUsername());
    }

    private String roleName(String code) {
        for (RoleCode role : RoleCode.values()) {
            if (role.getCode().equals(code)) {
                return role.getName();
            }
        }
        return code;
    }

    /** 手机号脱敏：138****8000；长度不足时原样返回。 */
    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 从审计 detail JSON 中提取 message 字段；非 JSON 时原样返回。 */
    private String extractMessage(String detail) {
        if (!StringUtils.hasText(detail)) {
            return null;
        }
        try {
            String message = JSONUtil.parseObj(detail).getStr("message");
            return StringUtils.hasText(message) ? message : detail;
        } catch (Exception e) {
            return detail;
        }
    }

    /** 防 LIKE 通配符注入（%、_、转义符）。 */
    private String escapeSqlLike(String keyword) {
        return keyword.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
