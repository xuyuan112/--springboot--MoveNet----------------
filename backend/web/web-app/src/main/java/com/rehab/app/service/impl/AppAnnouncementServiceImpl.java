package com.rehab.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rehab.app.service.AppAnnouncementService;
import com.rehab.app.vo.AppAnnouncementVO;
import com.rehab.model.entity.Announcement;
import com.rehab.model.enums.AnnouncementScope;
import com.rehab.model.enums.AnnouncementStatus;
import com.rehab.model.mapper.content.AnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 患者/治疗师端公告只读服务实现。
 */
@Service
@RequiredArgsConstructor
public class AppAnnouncementServiceImpl implements AppAnnouncementService {

    /** 端侧公告拉取上限（公告为低频运营内容） */
    private static final int MAX_LIMIT = 50;

    private final AnnouncementMapper announcementMapper;

    @Override
    public List<AppAnnouncementVO> listForPatient() {
        return listValid(AnnouncementScope.ALL.getCode(), AnnouncementScope.PATIENT.getCode());
    }

    @Override
    public List<AppAnnouncementVO> listForTherapist() {
        return listValid(AnnouncementScope.ALL.getCode(), AnnouncementScope.THERAPIST.getCode());
    }

    /**
     * 硬条件：已发布 + 当前时间在有效期内 + 可见范围命中；
     * 逻辑删除（deleted=1）由 @TableLogic 自动过滤。
     */
    private List<AppAnnouncementVO> listValid(String... scopes) {
        LocalDateTime now = LocalDateTime.now();
        List<Announcement> list = announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getStatus, AnnouncementStatus.PUBLISHED.getCode())
                        .in(Announcement::getScope, List.of(scopes))
                        .le(Announcement::getStartTime, now)
                        .ge(Announcement::getEndTime, now)
                        .orderByDesc(Announcement::getStartTime)
                        .last("LIMIT " + MAX_LIMIT));
        return list.stream().map(this::toVO).toList();
    }

    private AppAnnouncementVO toVO(Announcement entity) {
        AppAnnouncementVO vo = new AppAnnouncementVO();
        vo.setAnnouncementId(entity.getAnnouncementId());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
