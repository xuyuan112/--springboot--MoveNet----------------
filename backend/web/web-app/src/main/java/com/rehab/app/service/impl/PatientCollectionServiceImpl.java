package com.rehab.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.app.service.PatientCollectionService;
import com.rehab.app.vo.PatientCollectionVO;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.common.storage.MinioFileService;
import com.rehab.model.entity.ContentAttachment;
import com.rehab.model.entity.ContentCollection;
import com.rehab.model.entity.FileObject;
import com.rehab.model.entity.HealthContent;
import com.rehab.model.entity.RehabPartDict;
import com.rehab.model.enums.AttachmentType;
import com.rehab.model.enums.ContentStatus;
import com.rehab.model.enums.ContentType;
import com.rehab.model.mapper.content.ContentAttachmentMapper;
import com.rehab.model.mapper.content.ContentCollectionMapper;
import com.rehab.model.mapper.content.HealthContentMapper;
import com.rehab.model.mapper.system.FileObjectMapper;
import com.rehab.model.mapper.system.RehabPartDictMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 患者内容收藏服务实现（P5-B3）。
 *
 * <p>幂等约定：重复收藏命中唯一约束 uk_collection_user_content 时按成功处理（200）；
 * 取消收藏未命中也返回成功。内容必须当前 PUBLISHED 且在有效期内才可新增收藏；
 * 下架/过期后历史收藏保留但 available=false，不展示摘要与封面，详情接口 404。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientCollectionServiceImpl implements PatientCollectionService {

    private final ContentCollectionMapper contentCollectionMapper;
    private final HealthContentMapper healthContentMapper;
    private final ContentAttachmentMapper contentAttachmentMapper;
    private final FileObjectMapper fileObjectMapper;
    private final RehabPartDictMapper rehabPartDictMapper;
    private final ObjectProvider<MinioFileService> minioFileServiceProvider;

    @Override
    public PageResult<PatientCollectionVO> pageMyCollections(Integer page, Integer size) {
        Long userId = SecurityUtils.getUserId();
        Page<ContentCollection> mpPage = new Page<>(page, size);
        contentCollectionMapper.selectPage(mpPage, new LambdaQueryWrapper<ContentCollection>()
                .eq(ContentCollection::getUserId, userId)
                .orderByDesc(ContentCollection::getCollectedAt)
                .orderByDesc(ContentCollection::getCollectionId));
        if (mpPage.getRecords().isEmpty()) {
            return PageResult.of(mpPage, List.of());
        }
        Map<Long, HealthContent> contentMap = healthContentMapper.selectBatchIds(
                        mpPage.getRecords().stream().map(ContentCollection::getContentId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(HealthContent::getContentId, Function.identity()));
        Map<String, String> partNames = loadPartNames(contentMap.values().stream()
                .filter(this::isAvailable)
                .map(HealthContent::getTargetPartCode)
                .distinct()
                .toList());
        Map<Long, String> coverUrls = loadCoverUrls(contentMap.values().stream()
                .filter(this::isAvailable)
                .map(HealthContent::getContentId)
                .toList());

        List<PatientCollectionVO> voList = mpPage.getRecords().stream().map(collection -> {
            HealthContent content = contentMap.get(collection.getContentId());
            PatientCollectionVO vo = new PatientCollectionVO();
            vo.setCollectionId(collection.getCollectionId());
            vo.setContentId(collection.getContentId());
            vo.setCollectedAt(collection.getCollectedAt());
            boolean available = content != null && isAvailable(content);
            vo.setAvailable(available);
            if (content != null) {
                vo.setTitle(content.getTitle());
                vo.setContentType(content.getContentType());
                try {
                    vo.setContentTypeName(ContentType.fromCode(content.getContentType()).getName());
                } catch (IllegalArgumentException e) {
                    vo.setContentTypeName(content.getContentType());
                }
                vo.setTargetPartCode(content.getTargetPartCode());
                vo.setTargetPartName(partNames.getOrDefault(content.getTargetPartCode(), content.getTargetPartCode()));
                if (available) {
                    vo.setSummary(content.getSummary());
                    vo.setCoverUrl(coverUrls.get(content.getContentId()));
                }
            }
            return vo;
        }).toList();
        return PageResult.of(mpPage, voList);
    }

    @Override
    public void collect(Long contentId) {
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        HealthContent content = healthContentMapper.selectById(contentId);
        if (content == null || !isAvailable(content)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "内容不存在或暂未开放");
        }
        try {
            ContentCollection collection = new ContentCollection();
            collection.setUserId(userId);
            collection.setContentId(contentId);
            collection.setCollectedAt(now);
            contentCollectionMapper.insert(collection);
        } catch (DuplicateKeyException e) {
            // 重复收藏幂等：唯一约束 uk_collection_user_content 兜底，按成功处理
            log.debug("重复收藏幂等命中 userId={} contentId={}", userId, contentId);
        }
    }

    @Override
    public void uncollect(Long contentId) {
        Long userId = SecurityUtils.getUserId();
        contentCollectionMapper.delete(new LambdaQueryWrapper<ContentCollection>()
                .eq(ContentCollection::getUserId, userId)
                .eq(ContentCollection::getContentId, contentId));
    }

    // ==================== 私有辅助 ====================

    /** 可用判定：PUBLISHED 且当前时间在有效期内（validTo 为空表示长期）。 */
    private boolean isAvailable(HealthContent content) {
        LocalDateTime now = LocalDateTime.now();
        return ContentStatus.PUBLISHED.getCode().equals(content.getStatus())
                && content.getValidFrom() != null && !content.getValidFrom().isAfter(now)
                && (content.getValidTo() == null || !content.getValidTo().isBefore(now));
    }

    private Map<String, String> loadPartNames(List<String> codes) {
        if (codes.isEmpty()) {
            return Map.of();
        }
        return rehabPartDictMapper.selectBatchIds(codes).stream()
                .collect(Collectors.toMap(RehabPartDict::getCode, RehabPartDict::getName));
    }

    /** 批量生成可用内容的首张封面预签名地址（不可用内容不生成）。 */
    private Map<Long, String> loadCoverUrls(List<Long> contentIds) {
        if (contentIds.isEmpty()) {
            return Map.of();
        }
        List<ContentAttachment> covers = contentAttachmentMapper.selectList(
                new LambdaQueryWrapper<ContentAttachment>()
                        .in(ContentAttachment::getContentId, contentIds)
                        .eq(ContentAttachment::getAttachmentType, AttachmentType.COVER.getCode())
                        .orderByAsc(ContentAttachment::getSortNo)
                        .orderByAsc(ContentAttachment::getContentAttachmentId));
        if (covers.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> coverFileByContent = new HashMap<>();
        for (ContentAttachment cover : covers) {
            coverFileByContent.putIfAbsent(cover.getContentId(), cover.getFileId());
        }
        Map<Long, FileObject> fileMap = fileObjectMapper.selectBatchIds(coverFileByContent.values()).stream()
                .filter(f -> f.getDeletedAt() == null)
                .collect(Collectors.toMap(FileObject::getFileId, Function.identity()));
        MinioFileService minio = minioFileServiceProvider.getIfAvailable();
        Map<Long, String> result = new HashMap<>();
        coverFileByContent.forEach((contentId, fileId) -> {
            FileObject file = fileMap.get(fileId);
            if (file != null && minio != null) {
                try {
                    result.put(contentId, minio.presignedGetUrl(file.getBucketName(), file.getObjectKey()));
                } catch (IllegalStateException ignored) {
                    // 封面签名失败不阻断列表
                }
            }
        });
        return result;
    }
}
