package com.rehab.model.mapper.account;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.PatientTherapist;
import org.apache.ibatis.annotations.Mapper;

/**
 * patient_therapist 数据访问层。
 */
@Mapper
public interface PatientTherapistMapper extends BaseMapper<PatientTherapist> {
}