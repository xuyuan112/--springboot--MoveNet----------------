package com.rehab.model.mapper.action;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.ActionVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * action_version 数据访问层。
 */
@Mapper
public interface ActionVersionMapper extends BaseMapper<ActionVersion> {

    /**
     * 查询指定动作的最大版本号。
     */
    @Select("SELECT MAX(version_no) FROM action_version WHERE action_id = #{actionId} AND deleted = 0")
    Integer selectMaxVersionNo(@Param("actionId") Long actionId);
}
