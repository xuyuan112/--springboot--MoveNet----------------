package com.rehab.app.service;

import com.rehab.app.vo.PatientProfileVO;
import com.rehab.app.vo.PatientTherapistVO;
import com.rehab.model.dto.PatientProfileUpdateRequest;

/**
 * 患者端个人资料服务。
 */
public interface PatientProfileService {

    /**
     * 查询当前登录患者的完整资料。
     *
     * @return 本人资料（手机号脱敏）
     */
    PatientProfileVO getMyProfile();

    /**
     * 更新当前登录患者的开放字段资料。
     *
     * @param request 更新请求
     * @return 更新后的本人资料（脱敏）
     */
    PatientProfileVO updateMyProfile(PatientProfileUpdateRequest request);

    /**
     * 查询当前患者的有效负责治疗师。
     *
     * @return 有效绑定治疗师信息；无有效绑定时返回 null
     */
    PatientTherapistVO getMyTherapist();
}
