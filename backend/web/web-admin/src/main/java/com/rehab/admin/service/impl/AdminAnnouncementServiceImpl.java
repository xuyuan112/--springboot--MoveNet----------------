package com.rehab.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rehab.admin.service.AdminAnnouncementService;
import com.rehab.admin.vo.AdminAnnouncementVO;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.dto.AnnouncementPublishRequest;
import com.rehab.model.dto.AnnouncementSaveRequest;
import com.rehab.model.entity.Announcement;
import com.rehab.model.entity.UserAccount;
import com.rehab.model.enums.AnnouncementScope;
import com.rehab.model.enums.AnnouncementStatus;
import com.rehab.model.mapper.account.UserAccountMapper;
import com.rehab.model.mapper.content.AnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端 - 平台公告服务实现。
 */
@Service
@RequiredArgsConstructor
public class AdminAnnouncementServiceImpl
        extends ServiceImpl<AnnouncementMapper, Announcement>
        implements AdminAnnouncementService {

    /** 草稿未指定有效期时的默认展示时长：30 天 */
    private static final long DEFAULT_VALID_DAYS = 30L;

    private final UserAccountMapper userAccountMapper;

    @Override
    public PageResult<AdminAnnouncementVO> pageAnnouncements(Integer page, Integer size,
                                                             String status, String scope, String keyword) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(StringUtils.hasText(status), Announcement::getStatus, status)
                .eq(StringUtils.hasText(scope), Announcement::getScope, scope)
                .like(StringUtils.hasText(keyword), Announcement::getTitle, keyword)
                .orderByDesc(Announcement::getAnnouncementId);
        Page<Announcement> result = this.page(new Page<>(page, size), wrapper);
        List<AdminAnnouncementVO> voList = result.getRecords().stream().map(this::toVO).toList();
        fillCreatorNames(voList);
        return PageResult.of(result, voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAnnouncement(AnnouncementSaveRequest request) {
        validatePeriod(request.getStartTime(), request.getEndTime());
        LocalDateTime now = LocalDateTime.now();
        Announcement entity = new Announcement();
        entity.setTitle(request.getTitle().trim());
        entity.setContent(request.getContent());
        entity.setScope(request.getScope());
        entity.setStatus(AnnouncementStatus.DRAFT.getCode());
        entity.setStartTime(request.getStartTime() != null ? request.getStartTime() : now);
        entity.setEndTime(request.getEndTime() != null ? request.getEndTime() : now.plusDays(DEFAULT_VALID_DAYS));
        entity.setCreatedBy(SecurityUtils.getUserId());
        this.save(entity);
        return entity.getAnnouncementId();
    }

    @Override
    public AdminAnnouncementVO getAnnouncement(Long announcementId) {
        Announcement entity = requireAnnouncement(announcementId);
        AdminAnnouncementVO vo = toVO(entity);
        fillCreatorNames(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAnnouncement(Long announcementId, AnnouncementSaveRequest request) {
        Announcement entity = requireAnnouncement(announcementId);
        if (AnnouncementStatus.PUBLISHED.getCode().equals(entity.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT,
                    "已发布公告不可编辑，请先下架再修改");
        }
        validatePeriod(request.getStartTime(), request.getEndTime());
        entity.setTitle(request.getTitle().trim());
        entity.setContent(request.getContent());
        entity.setScope(request.getScope());
        if (request.getStartTime() != null) {
            entity.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            entity.setEndTime(request.getEndTime());
        }
        if (!entity.getEndTime().isAfter(entity.getStartTime())
                && !entity.getEndTime().isEqual(entity.getStartTime())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "展示结束时间不能早于开始时间");
        }
        this.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long announcementId, AnnouncementPublishRequest request) {
        Announcement entity = requireAnnouncement(announcementId);
        if (AnnouncementStatus.PUBLISHED.getCode().equals(entity.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "公告已发布，请勿重复发布");
        }
        validatePeriod(request.getStartTime(), request.getEndTime());
        entity.setStatus(AnnouncementStatus.PUBLISHED.getCode());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        this.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long announcementId) {
        Announcement entity = requireAnnouncement(announcementId);
        if (!AnnouncementStatus.PUBLISHED.getCode().equals(entity.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT,
                    "仅已发布公告可下架，当前状态：" + entity.getStatus());
        }
        entity.setStatus(AnnouncementStatus.OFFLINE.getCode());
        this.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAnnouncement(Long announcementId) {
        Announcement entity = requireAnnouncement(announcementId);
        if (AnnouncementStatus.PUBLISHED.getCode().equals(entity.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "已发布公告请先下架再删除");
        }
        this.removeById(announcementId);
    }

    // ==================== 内部辅助 ====================

    private Announcement requireAnnouncement(Long announcementId) {
        Announcement entity = this.getById(announcementId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在：" + announcementId);
        }
        return entity;
    }

    private void validatePeriod(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "展示结束时间不能早于开始时间");
        }
    }

    private AdminAnnouncementVO toVO(Announcement entity) {
        AdminAnnouncementVO vo = new AdminAnnouncementVO();
        vo.setAnnouncementId(entity.getAnnouncementId());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setScope(entity.getScope());
        vo.setScopeName(resolveScopeName(entity.getScope()));
        vo.setStatus(entity.getStatus());
        vo.setStatusName(resolveStatusName(entity.getStatus()));
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String resolveScopeName(String code) {
        try {
            return AnnouncementScope.fromCode(code).getName();
        } catch (IllegalArgumentException e) {
            return code;
        }
    }

    private String resolveStatusName(String code) {
        try {
            return AnnouncementStatus.fromCode(code).getName();
        } catch (IllegalArgumentException e) {
            return code;
        }
    }

    /** 批量填充创建人账号，避免 N+1 */
    private void fillCreatorNames(List<AdminAnnouncementVO> voList) {
        Set<Long> userIds = voList.stream()
                .map(AdminAnnouncementVO::getCreatedBy)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = userAccountMapper.selectList(
                        new LambdaQueryWrapper<UserAccount>().in(UserAccount::getUserId, userIds))
                .stream()
                .collect(Collectors.toMap(UserAccount::getUserId, UserAccount::getUsername, (a, b) -> a));
        voList.forEach(vo -> vo.setCreatorName(nameMap.get(vo.getCreatedBy())));
    }
}
