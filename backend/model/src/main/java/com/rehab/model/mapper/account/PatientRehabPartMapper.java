package com.rehab.model.mapper.account;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.PatientRehabPart;
import org.apache.ibatis.annotations.Mapper;

/**
 * patient_rehab_part 数据访问层。
 */
@Mapper
public interface PatientRehabPartMapper extends BaseMapper<PatientRehabPart> {
}