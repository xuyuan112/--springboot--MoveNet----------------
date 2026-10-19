package com.rehab.model.mapper.consultation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.ConsultationMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * consultation_message 数据访问层。
 */
@Mapper
public interface ConsultationMessageMapper extends BaseMapper<ConsultationMessage> {
}