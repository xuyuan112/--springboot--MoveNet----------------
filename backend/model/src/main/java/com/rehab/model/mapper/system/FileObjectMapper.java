package com.rehab.model.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.FileObject;
import org.apache.ibatis.annotations.Mapper;

/**
 * file_object 数据访问层。
 */
@Mapper
public interface FileObjectMapper extends BaseMapper<FileObject> {
}