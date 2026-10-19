package com.rehab.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rehab.app.service.PatientProfileService;
import com.rehab.app.vo.PatientProfileVO;
import com.rehab.app.vo.PatientTherapistVO;
import com.rehab.common.constant.Constants;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.dto.PatientProfileUpdateRequest;
import com.rehab.model.entity.PatientProfile;
import com.rehab.model.entity.PatientRehabPart;
import com.rehab.model.entity.PatientTherapist;
import com.rehab.model.entity.RehabPartDict;
import com.rehab.model.entity.TherapistProfile;
import com.rehab.model.entity.UserAccount;
import com.rehab.model.enums.BindingStatus;
import com.rehab.model.mapper.account.PatientProfileMapper;
import com.rehab.model.mapper.account.PatientRehabPartMapper;
import com.rehab.model.mapper.account.PatientTherapistMapper;
import com.rehab.model.mapper.account.TherapistProfileMapper;
import com.rehab.model.mapper.account.UserAccountMapper;
import com.rehab.model.mapper.system.RehabPartDictMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 患者端个人资料服务实现。
 */
@Service
@RequiredArgsConstructor
public class PatientProfileServiceImpl implements PatientProfileService {

    private final UserAccountMapper userAccountMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final PatientRehabPartMapper patientRehabPartMapper;
    private final PatientTherapistMapper patientTherapistMapper;
    private final TherapistProfileMapper therapistProfileMapper;
    private final RehabPartDictMapper rehabPartDictMapper;

    @Override
    public PatientProfileVO getMyProfile() {
        Long userId = SecurityUtils.getUserId();
        UserAccount user = requireUser(userId);
        PatientProfile profile = patientProfileMapper.selectOne(new LambdaQueryWrapper<PatientProfile>()
                .eq(PatientProfile::getUserId, userId)
                .last("limit 1"));
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "患者档案不存在");
        }
        return toVO(user, profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PatientProfileVO updateMyProfile(PatientProfileUpdateRequest request) {
        Long userId = SecurityUtils.getUserId();
        UserAccount user = requireUser(userId);
        PatientProfile profile = patientProfileMapper.selectOne(new LambdaQueryWrapper<PatientProfile>()
                .eq(PatientProfile::getUserId, userId)
                .last("limit 1"));
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "患者档案不存在");
        }

        // 字段级更新：仅放行允许修改的开放字段
        if (StringUtils.hasText(request.getName())) {
            profile.setRealName(request.getName().trim());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender().getCode());
        }
        if (request.getBirthday() != null) {
            profile.setBirthDate(request.getBirthday());
        }
        if (request.getHeightCm() != null) {
            profile.setHeightCm(request.getHeightCm());
        }
        if (request.getWeightKg() != null) {
            profile.setWeightKg(request.getWeightKg());
        }
        if (request.getMedicalSummary() != null) {
            profile.setMedicalHistory(request.getMedicalSummary());
        }
        if (request.getAllergyInfo() != null) {
            profile.setAllergyInfo(request.getAllergyInfo());
        }
        if (request.getContraindications() != null) {
            profile.setContraindication(request.getContraindications());
        }
        patientProfileMapper.updateById(profile);

        // 主康复部位：更新或插入（保留历史，primaryFlag 切换）
        if (StringUtils.hasText(request.getRehabPartCode())) {
            upsertPrimaryRehabPart(userId, request.getRehabPartCode().trim());
        }

        return toVO(user, profile);
    }

    @Override
    public PatientTherapistVO getMyTherapist() {
        Long userId = SecurityUtils.getUserId();
        PatientTherapist binding = patientTherapistMapper.selectOne(new LambdaQueryWrapper<PatientTherapist>()
                .eq(PatientTherapist::getPatientId, userId)
                .eq(PatientTherapist::getStatus, BindingStatus.ACTIVE.getCode())
                .orderByDesc(PatientTherapist::getRelationType) // PRIMARY > SECONDARY，字母序降序
                .orderByDesc(PatientTherapist::getStartAt)
                .last("limit 1"));
        if (binding == null) {
            return null;
        }
        TherapistProfile tp = therapistProfileMapper.selectOne(new LambdaQueryWrapper<TherapistProfile>()
                .eq(TherapistProfile::getUserId, binding.getTherapistId())
                .last("limit 1"));
        if (tp == null) {
            return null;
        }
        PatientTherapistVO vo = new PatientTherapistVO();
        vo.setBindingId(binding.getBindingId());
        vo.setTherapistId(binding.getTherapistId());
        vo.setName(tp.getRealName());
        vo.setTitle(tp.getTitle());
        vo.setSpecialty(tp.getSpecialty());
        vo.setIntroduction(tp.getIntroduction());
        vo.setRelationType(binding.getRelationType());
        vo.setStartAt(binding.getStartAt());
        return vo;
    }

    // ==================== 私有辅助 ====================

    private UserAccount requireUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.AUTH_REQUIRED);
        }
        return user;
    }

    /**
     * 更新/插入患者主康复部位：仅允许启用字典编码；
     * 同一患者同一部位已存在 ACTIVE 记录则直接置为主部位，否则插入新记录；
     * 其他部位自动取消主部位标记。
     */
    private void upsertPrimaryRehabPart(Long patientId, String partCode) {
        RehabPartDict dict = rehabPartDictMapper.selectById(partCode);
        if (dict == null || !Constants.ENABLED.equals(dict.getEnabled())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "康复部位编码不存在或已停用：" + partCode);
        }

        // 清除其他主部位标记
        patientRehabPartMapper.update(null, new LambdaUpdateWrapper<PatientRehabPart>()
                .eq(PatientRehabPart::getPatientId, patientId)
                .eq(PatientRehabPart::getPrimaryFlag, 1)
                .ne(PatientRehabPart::getRehabPartCode, partCode)
                .set(PatientRehabPart::getPrimaryFlag, 0));

        // 查询是否已存在该部位的 ACTIVE 记录
        PatientRehabPart existing = patientRehabPartMapper.selectOne(new LambdaQueryWrapper<PatientRehabPart>()
                .eq(PatientRehabPart::getPatientId, patientId)
                .eq(PatientRehabPart::getRehabPartCode, partCode)
                .eq(PatientRehabPart::getStatus, "ACTIVE")
                .last("limit 1"));
        if (existing != null) {
            existing.setPrimaryFlag(1);
            patientRehabPartMapper.updateById(existing);
        } else {
            PatientRehabPart record = new PatientRehabPart();
            record.setPatientId(patientId);
            record.setRehabPartCode(partCode);
            record.setSide("BILATERAL");
            record.setPrimaryFlag(1);
            record.setStatus("ACTIVE");
            patientRehabPartMapper.insert(record);
        }
    }

    private PatientProfileVO toVO(UserAccount user, PatientProfile profile) {
        PatientProfileVO vo = new PatientProfileVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setName(profile.getRealName());
        vo.setGender(profile.getGender());
        vo.setBirthday(profile.getBirthDate());
        vo.setHeightCm(profile.getHeightCm());
        vo.setWeightKg(profile.getWeightKg());
        vo.setMedicalSummary(profile.getMedicalHistory());
        vo.setAllergyInfo(profile.getAllergyInfo());
        vo.setContraindications(profile.getContraindication());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setUpdatedAt(profile.getUpdatedAt());

        // 主康复部位
        PatientRehabPart primary = patientRehabPartMapper.selectOne(new LambdaQueryWrapper<PatientRehabPart>()
                .eq(PatientRehabPart::getPatientId, user.getUserId())
                .eq(PatientRehabPart::getPrimaryFlag, 1)
                .eq(PatientRehabPart::getStatus, "ACTIVE")
                .last("limit 1"));
        if (primary != null) {
            vo.setRehabPartCode(primary.getRehabPartCode());
            RehabPartDict dict = rehabPartDictMapper.selectById(primary.getRehabPartCode());
            vo.setRehabPartName(dict != null ? dict.getName() : null);
        }
        return vo;
    }

    /** 手机号脱敏：138****8000；长度不足时原样返回。 */
    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
