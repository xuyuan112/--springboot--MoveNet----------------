package com.rehab.model.mapper.plan;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.PlanDailyTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * plan_daily_task 数据访问层。
 */
@Mapper
public interface PlanDailyTaskMapper extends BaseMapper<PlanDailyTask> {
}