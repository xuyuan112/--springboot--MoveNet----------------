package com.rehab.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.model.mapper.content.ContentAttachmentMapper;
import com.rehab.model.mapper.system.FileObjectMapper;
import com.rehab.model.mapper.content.HealthContentMapper;
import com.rehab.model.mapper.system.RehabPartDictMapper;
import com.rehab.app.service.PatientContentService;
import com.rehab.app.vo.PatientContentDetailVO;
import com.rehab.app.vo.PatientContentVO;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.storage.MinioFileService;
import com.rehab.common.storage.MinioProperties;
import com.rehab.model.entity.ContentAttachment;
import com.rehab.model.entity.FileObject;
import com.rehab.model.entity.HealthContent;
import com.rehab.model.entity.RehabPartDict;
import com.rehab.model.enums.AttachmentType;
import com.rehab.model.enums.ContentStatus;
import com.rehab.model.enums.ContentType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 患者端 - 公共内容只读服务实现。
 *
 * <p>可见性硬条件：{@code status=PUBLISHED} 且当前时间落在 [validFrom, validTo]
 * （validTo 为空表示长期有效）；草稿、待审核、已下架、未到生效期、已过期内容均不可见，
 * 详情直接返回 404，避免泄露未审核内容（接口文档 3.5）。</p>
 */
@Service
@RequiredArgsConstructor
public class PatientContentServiceImpl implements PatientContentService {

    private final HealthContentMapper healthContentMapper;
    private final ContentAttachmentMapper contentAttachmentMapper;
    private final FileObjectMapper fileObjectMapper;
    private final RehabPartDictMapper rehabPartDictMapper;
    private final ObjectProvider<MinioFileService> minioFileServiceProvider;
    private final MinioProperties minioProperties;

    @Override
    public PageResult<PatientContentVO> pagePublished(Integer page, Integer size,
                                                      String partCode, String category, String keyword) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<HealthContent> wrapper = new LambdaQueryWrapper<HealthContent>()
                .eq(HealthContent::getStatus, ContentStatus.PUBLISHED.getCode())
                .le(HealthContent::getValidFrom, now)
                .and(w -> w.isNull(HealthContent::getValidTo).or().ge(HealthContent::getValidTo, now))
                .eq(StringUtils.hasText(partCode), HealthContent::getTargetPartCode, partCode)
                .eq(StringUtils.hasText(category), HealthContent::getContentType, category)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(HealthContent::getTitle, keyword)
                        .or().like(HealthContent::getSummary, keyword))
                .orderByDesc(HealthContent::getPublishedAt)
                .orderByDesc(HealthContent::getContentId);

        Page<HealthContent> result = healthContentMapper.selectPage(new Page<>(page, size), wrapper);
        if (result.getRecords().isEmpty()) {
            return PageResult.of(result, List.of());
        }

        List<HealthContent> contents = result.getRecords();
        Map<String, String> partNameMap = loadPartNames(contents);
        Map<Long, String> coverUrlMap = loadCoverUrls(
                contents.stream().map(HealthContent::getContentId).toList());

        List<PatientContentVO> voList = contents.stream()
                .map(c -> PatientContentVO.builder()
                        .contentId(c.getContentId())
                        .title(c.getTitle())
                        .contentType(c.getContentType())
                        .contentTypeName(ContentType.fromCode(c.getContentType()).getName())
                        .targetPartCode(c.getTargetPartCode())
                        .targetPartName(partNameMap.getOrDefault(c.getTargetPartCode(), c.getTargetPartCode()))
                        .targetGroup(c.getTargetGroup())
                        .summary(c.getSummary())
                        .coverUrl(coverUrlMap.get(c.getContentId()))
                        .validFrom(c.getValidFrom())
                        .validTo(c.getValidTo())
                        .publishedAt(c.getPublishedAt())
                        .build())
                .toList();
        return PageResult.of(result, voList);
    }

    @Override
    public PatientContentDetailVO getPublished(Long contentId) {
        LocalDateTime now = LocalDateTime.now();
        HealthContent content = healthContentMapper.selectOne(new LambdaQueryWrapper<HealthContent>()
                .eq(HealthContent::getContentId, contentId)
                .eq(HealthContent::getStatus, ContentStatus.PUBLISHED.getCode())
                .le(HealthContent::getValidFrom, now)
                .and(w -> w.isNull(HealthContent::getValidTo).or().ge(HealthContent::getValidTo, now)));
        if (content == null) {
            // 统一 404，不区分"不存在"与"存在但不可见"
            throw new BusinessException(ResultCode.NOT_FOUND, "内容不存在或暂未开放");
        }

        String partName = resolvePartName(content.getTargetPartCode());
        return PatientContentDetailVO.builder()
                .contentId(content.getContentId())
                .title(content.getTitle())
                .contentType(content.getContentType())
                .contentTypeName(ContentType.fromCode(content.getContentType()).getName())
                .targetPartCode(content.getTargetPartCode())
                .targetPartName(partName)
                .targetGroup(content.getTargetGroup())
                .summary(content.getSummary())
                .contentBody(content.getContentBody())
                .validFrom(content.getValidFrom())
                .validTo(content.getValidTo())
                .publishedAt(content.getPublishedAt())
                .attachments(buildAttachments(contentId))
                .build();
    }

    // ==================== 内部辅助 ====================

    /** 批量加载每条内容的首张封面预签名地址（N 条内容仅 2 次 SQL + N 次本地签名运算） */
    private Map<Long, String> loadCoverUrls(List<Long> contentIds) {
        List<ContentAttachment> covers = contentAttachmentMapper.selectList(
                new LambdaQueryWrapper<ContentAttachment>()
                        .in(ContentAttachment::getContentId, contentIds)
                        .eq(ContentAttachment::getAttachmentType, AttachmentType.COVER.getCode())
                        .orderByAsc(ContentAttachment::getSortNo)
                        .orderByAsc(ContentAttachment::getContentAttachmentId));
        if (covers.isEmpty()) {
            return Map.of();
        }
        // 每条内容只取排序第一的封面（SQL 已排序，putIfAbsent 保留首条）
        Map<Long, Long> contentCoverFileMap = new java.util.HashMap<>();
        for (ContentAttachment cover : covers) {
            contentCoverFileMap.putIfAbsent(cover.getContentId(), cover.getFileId());
        }
        Map<Long, FileObject> fileMap = loadActiveFiles(contentCoverFileMap.values());
        MinioFileService minio = minioFileServiceProvider.getIfAvailable();

        Map<Long, String> result = new java.util.HashMap<>();
        contentCoverFileMap.forEach((contentId, fileId) -> {
            FileObject file = fileMap.get(fileId);
            if (file != null && minio != null) {
                try {
                    result.put(contentId, minio.presignedGetUrl(file.getBucketName(), file.getObjectKey()));
                } catch (IllegalStateException ignored) {
                    // 封面地址生成失败不阻断列表
                }
            }
        });
        return result;
    }

    private List<PatientContentDetailVO.AttachmentVO> buildAttachments(Long contentId) {
        List<ContentAttachment> rows = contentAttachmentMapper.selectList(
                new LambdaQueryWrapper<ContentAttachment>()
                        .eq(ContentAttachment::getContentId, contentId)
                        .orderByAsc(ContentAttachment::getSortNo)
                        .orderByAsc(ContentAttachment::getContentAttachmentId));
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, FileObject> fileMap = loadActiveFiles(
                rows.stream().map(ContentAttachment::getFileId).toList());
        MinioFileService minio = minioFileServiceProvider.getIfAvailable();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(minioProperties.getPresignExpireSec());

        List<PatientContentDetailVO.AttachmentVO> result = new ArrayList<>(rows.size());
        for (ContentAttachment row : rows) {
            FileObject file = fileMap.get(row.getFileId());
            if (file == null) {
                continue;
            }
            AttachmentType type = AttachmentType.fromCode(row.getAttachmentType());
            PatientContentDetailVO.AttachmentVO.AttachmentVOBuilder builder = PatientContentDetailVO.AttachmentVO.builder()
                    .id(row.getContentAttachmentId())
                    .type(type.getCode())
                    .typeName(type.getName())
                    .name(file.getFileName())
                    .contentType(file.getContentType())
                    .size(file.getFileSize())
                    .urlExpiresAt(expiresAt);
            if (minio != null) {
                try {
                    builder.url(minio.presignedGetUrl(file.getBucketName(), file.getObjectKey()));
                } catch (IllegalStateException ignored) {
                    builder.url(null);
                }
            }
            result.add(builder.build());
        }
        return result;
    }

    private Map<Long, FileObject> loadActiveFiles(Iterable<Long> fileIds) {
        List<Long> idList = new ArrayList<>();
        fileIds.forEach(idList::add);
        if (idList.isEmpty()) {
            return Map.of();
        }
        return fileObjectMapper.selectBatchIds(idList).stream()
                .filter(f -> f.getDeletedAt() == null)
                .collect(Collectors.toMap(FileObject::getFileId, Function.identity()));
    }

    private Map<String, String> loadPartNames(List<HealthContent> contents) {
        List<String> codes = contents.stream()
                .map(HealthContent::getTargetPartCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (codes.isEmpty()) {
            return Map.of();
        }
        return rehabPartDictMapper.selectBatchIds(codes).stream()
                .collect(Collectors.toMap(RehabPartDict::getCode, RehabPartDict::getName));
    }

    private String resolvePartName(String code) {
        if (!StringUtils.hasText(code)) {
            return code;
        }
        RehabPartDict dict = rehabPartDictMapper.selectById(code);
        return dict != null ? dict.getName() : code;
    }
}
