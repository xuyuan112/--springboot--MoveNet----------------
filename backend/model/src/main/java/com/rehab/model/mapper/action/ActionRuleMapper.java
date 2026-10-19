package com.rehab.model.mapper.action;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.ActionRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * action_rule 数据访问层。
 */
@Mapper
public interface ActionRuleMapper extends BaseMapper<ActionRule> {

    /**
     * 查询指定版本下所有规则的评分权重之和。
     */
    @Select("SELECT COALESCE(SUM(score_weight), 0) FROM action_rule WHERE action_version_id = #{versionId} AND deleted = 0")
    BigDecimal sumScoreWeightByVersionId(@Param("versionId") Long versionId);
}
