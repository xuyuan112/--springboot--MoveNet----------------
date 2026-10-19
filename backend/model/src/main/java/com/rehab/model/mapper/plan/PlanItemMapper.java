package com.rehab.model.mapper.plan;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.PlanItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * plan_item 数据访问层。
 */
@Mapper
public interface PlanItemMapper extends BaseMapper<PlanItem> {

    /**
     * 物理删除计划下全部动作项（随 DRAFT 计划删除或草稿期重建动作项时使用；
     * uk_plan_item_sequence(plan_id, sequence_no) 会因逻辑删除残留导致顺序号无法复用）。
     *
     * @param planId 计划ID
     * @return 影响行数
     */
    @Delete("DELETE FROM plan_item WHERE plan_id = #{planId}")
    int deleteByPlanIdPhysically(@Param("planId") Long planId);

    /**
     * 按 ID 物理删除单个动作项（仅草稿计划；uk_plan_item_sequence 占位问题同上）。
     *
     * @param planItemId 计划项ID
     * @return 影响行数
     */
    @Delete("DELETE FROM plan_item WHERE plan_item_id = #{planItemId}")
    int deleteByIdPhysically(@Param("planItemId") Long planItemId);
}
