package com.rehab.model.mapper.plan;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.PlanSchedule;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * plan_schedule 数据访问层。
 */
@Mapper
public interface PlanScheduleMapper extends BaseMapper<PlanSchedule> {

    /**
     * 物理删除计划下全部执行星期（uk_schedule_day(plan_id, weekday) 同理需物理清除）。
     *
     * @param planId 计划ID
     * @return 影响行数
     */
    @Delete("DELETE FROM plan_schedule WHERE plan_id = #{planId}")
    int deleteByPlanIdPhysically(@Param("planId") Long planId);
}
