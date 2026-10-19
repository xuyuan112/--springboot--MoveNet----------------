package com.rehab.model.mapper.training;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.TrainingRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * training_record 数据访问层。
 */
@Mapper
public interface TrainingRecordMapper extends BaseMapper<TrainingRecord> {
}