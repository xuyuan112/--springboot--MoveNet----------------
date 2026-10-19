package com.rehab.model.mapper.plan;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.RehabPlan;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * rehab_plan 数据访问层。
 */
@Mapper
public interface RehabPlanMapper extends BaseMapper<RehabPlan> {

    /**
     * 物理删除计划（仅限 DRAFT 草稿：无任务/记录/报告引用）。
     * 逻辑删除会在 uk_plan_version(patient_id, name, version_no) 中留下占位，
     * 导致同名计划重建时唯一键冲突，故草稿删除走物理删除。
     *
     * @param planId 计划ID
     * @return 影响行数
     */
    @Delete("DELETE FROM rehab_plan WHERE plan_id = #{planId}")
    int deleteDraftPhysically(@Param("planId") Long planId);
}
