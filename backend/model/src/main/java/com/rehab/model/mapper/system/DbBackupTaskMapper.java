package com.rehab.model.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.DbBackupTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据库备份恢复任务 Mapper
 */
@Mapper
public interface DbBackupTaskMapper extends BaseMapper<DbBackupTask> {
}
