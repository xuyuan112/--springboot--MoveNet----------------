package com.rehab.model.mapper.training;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.TrainingAnnotation;
import org.apache.ibatis.annotations.Mapper;

/**
 * rehab_training_annotation 数据访问层。
 */
@Mapper
public interface TrainingAnnotationMapper extends BaseMapper<TrainingAnnotation> {
}
