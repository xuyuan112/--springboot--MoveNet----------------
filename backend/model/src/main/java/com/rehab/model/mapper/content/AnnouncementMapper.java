package com.rehab.model.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.model.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

/**
 * announcement 数据访问层。
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}