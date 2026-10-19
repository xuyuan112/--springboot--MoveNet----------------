package com.rehab.model.mapper.action;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.Action;
import org.apache.ibatis.annotations.Mapper;

/**
 * action 数据访问层。
 */
@Mapper
public interface ActionMapper extends BaseMapper<Action> {
}