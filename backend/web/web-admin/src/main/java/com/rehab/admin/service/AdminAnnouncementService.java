package com.rehab.admin.service;

import com.rehab.common.result.PageResult;
import com.rehab.admin.vo.AdminAnnouncementVO;
import com.rehab.model.dto.AnnouncementPublishRequest;
import com.rehab.model.dto.AnnouncementSaveRequest;

/**
 * 管理端 - 平台公告服务。
 *
 * <p>状态机：DRAFT → PUBLISHED → OFFLINE（OFFLINE 可重新发布回到 PUBLISHED）；
 * 编辑/删除仅允许 DRAFT、OFFLINE；PUBLISHED 须先下架。</p>
 */
public interface AdminAnnouncementService {

    /** 分页查询公告（status/scope/keyword 可选） */
    PageResult<AdminAnnouncementVO> pageAnnouncements(Integer page, Integer size,
                                                      String status, String scope, String keyword);

    /** 创建公告草稿，返回公告ID */
    Long createAnnouncement(AnnouncementSaveRequest request);

    /** 查询公告详情 */
    AdminAnnouncementVO getAnnouncement(Long announcementId);

    /** 修改公告（仅 DRAFT/OFFLINE） */
    void updateAnnouncement(Long announcementId, AnnouncementSaveRequest request);

    /** 发布公告并设置有效期（DRAFT/OFFLINE → PUBLISHED） */
    void publish(Long announcementId, AnnouncementPublishRequest request);

    /** 下架公告（PUBLISHED → OFFLINE） */
    void offline(Long announcementId);

    /** 删除公告（仅 DRAFT/OFFLINE，逻辑删除） */
    void deleteAnnouncement(Long announcementId);
}
