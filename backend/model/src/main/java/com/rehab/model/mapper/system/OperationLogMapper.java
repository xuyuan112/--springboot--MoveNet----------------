package com.rehab.model.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * operation_log 数据访问层。
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}