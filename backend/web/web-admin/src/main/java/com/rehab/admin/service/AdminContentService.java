package com.rehab.admin.service;

import com.rehab.common.result.PageResult;
import com.rehab.model.dto.ContentAuditRequest;
import com.rehab.model.dto.HealthContentSaveRequest;
import com.rehab.admin.vo.AdminContentDetailVO;
import com.rehab.admin.vo.AdminContentVO;

/**
 * 管理端 - 公共健康内容服务：CRUD + 审核状态机。
 */
public interface AdminContentService {

    /** 分页查询内容（status 状态、category 内容类型、keyword 标题/摘要模糊） */
    PageResult<AdminContentVO> pageContents(Integer page, Integer size,
                                            String status, String category, String keyword);

    /** 创建内容草稿 */
    Long createContent(HealthContentSaveRequest request);

    /** 查询内容详情（含附件预签名地址） */
    AdminContentDetailVO detail(Long contentId);

    /** 编辑内容（仅 DRAFT 可编辑，附件全量替换） */
    void updateContent(Long contentId, HealthContentSaveRequest request);

    /** 审核：APPROVE 通过 / REJECT 驳回（理由必填，回退 DRAFT）/ OFFLINE 下架 */
    void audit(Long contentId, ContentAuditRequest request);

    /** 删除未发布草稿（已发布内容须先下架） */
    void deleteContent(Long contentId);
}
