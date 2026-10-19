package com.rehab.app.service;

import com.rehab.common.result.PageResult;
import com.rehab.app.vo.PatientContentDetailVO;
import com.rehab.app.vo.PatientContentVO;

/**
 * 患者端 - 公共内容只读服务：仅返回 PUBLISHED 且当前在有效期内的内容。
 * 收藏关系不在本服务（随患者咨询/内容模块一并实现）。
 */
public interface PatientContentService {

    /** 分页查询已发布且在有效期内的内容 */
    PageResult<PatientContentVO> pagePublished(Integer page, Integer size,
                                               String partCode, String category, String keyword);

    /** 查询已发布内容详情（含附件预签名地址）；不可见内容一律 404 */
    PatientContentDetailVO getPublished(Long contentId);
}
