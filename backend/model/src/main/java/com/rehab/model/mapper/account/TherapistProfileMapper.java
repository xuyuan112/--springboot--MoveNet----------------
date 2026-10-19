package com.rehab.model.mapper.account;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.TherapistProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * therapist_profile 数据访问层。
 */
@Mapper
public interface TherapistProfileMapper extends BaseMapper<TherapistProfile> {
}