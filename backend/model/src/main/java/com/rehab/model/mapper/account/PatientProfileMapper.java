package com.rehab.model.mapper.account;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.PatientProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * patient_profile 数据访问层。
 */
@Mapper
public interface PatientProfileMapper extends BaseMapper<PatientProfile> {
}