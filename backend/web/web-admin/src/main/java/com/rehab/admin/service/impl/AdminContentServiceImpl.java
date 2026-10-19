package com.rehab.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rehab.admin.service.AdminContentService;
import com.rehab.admin.vo.AdminContentDetailVO;
import com.rehab.admin.vo.AdminContentVO;
import com.rehab.admin.vo.ContentAttachmentVO;
import com.rehab.model.mapper.content.ContentAttachmentMapper;
import com.rehab.model.mapper.content.ContentCollectionMapper;
import com.rehab.model.mapper.system.FileObjectMapper;
import com.rehab.model.mapper.content.HealthContentMapper;
import com.rehab.model.mapper.system.RehabPartDictMapper;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.common.storage.MinioFileService;
import com.rehab.common.storage.MinioProperties;
import com.rehab.model.dto.ContentAuditRequest;
import com.rehab.model.dto.HealthContentSaveRequest;
import com.rehab.model.entity.ContentAttachment;
import com.rehab.model.entity.ContentCollection;
import com.rehab.model.entity.FileObject;
import com.rehab.model.entity.HealthContent;
import com.rehab.model.entity.RehabPartDict;
import com.rehab.model.enums.AttachmentType;
import com.rehab.model.enums.ContentStatus;
import com.rehab.model.enums.ContentType;
import com.rehab.model.enums.ScanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端 - 公共健康内容服务实现。
 *
 * <p><b>状态机（以 ContentStatus 枚举为准）</b>：代码枚举为
 * DRAFT/PENDING_REVIEW/PUBLISHED/OFFLINE，无文档文字中的 REJECTED 态；
 * REJECT 回退 DRAFT 并写 reviewRemark（驳回理由），内容可再次编辑。</p>
 * <ul>
 *   <li>APPROVE：DRAFT / PENDING_REVIEW → PUBLISHED</li>
 *   <li>REJECT：DRAFT / PENDING_REVIEW → DRAFT（comment 必填）</li>
 *   <li>OFFLINE：PUBLISHED → OFFLINE</li>
 *   <li>PUT：仅 DRAFT</li>
 *   <li>DELETE：DRAFT / PENDING_REVIEW / OFFLINE（已发布内容须先下架再删）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AdminContentServiceImpl extends ServiceImpl<HealthContentMapper, HealthContent>
        implements AdminContentService {

    private final RehabPartDictMapper rehabPartDictMapper;
    private final ContentAttachmentMapper contentAttachmentMapper;
    private final ContentCollectionMapper contentCollectionMapper;
    private final FileObjectMapper fileObjectMapper;
    private final ObjectProvider<MinioFileService> minioFileServiceProvider;
    private final MinioProperties minioProperties;

    @Override
    public PageResult<AdminContentVO> pageContents(Integer page, Integer size,
                                                   String status, String category, String keyword) {
        LambdaQueryWrapper<HealthContent> wrapper = new LambdaQueryWrapper<HealthContent>()
                .eq(StringUtils.hasText(status), HealthContent::getStatus, status)
                .eq(StringUtils.hasText(category), HealthContent::getContentType, category)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(HealthContent::getTitle, keyword)
                        .or().like(HealthContent::getSummary, keyword))
                .orderByDesc(HealthContent::getContentId);

        Page<HealthContent> result = this.page(new Page<>(page, size), wrapper);
        if (result.getRecords().isEmpty()) {
            return PageResult.of(result, List.of());
        }
        Map<String, String> partNameMap = loadPartNames(result.getRecords());
        List<AdminContentVO> voList = result.getRecords().stream()
                .map(c -> toListVO(c, partNameMap))
                .toList();
        return PageResult.of(result, voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContent(HealthContentSaveRequest request) {
        requirePartExists(request.getTargetPartCode());
        LocalDateTime validFrom = request.getValidFrom() != null ? request.getValidFrom() : LocalDateTime.now();
        validatePeriod(validFrom, request.getValidTo());

        HealthContent content = new HealthContent();
        applyEditableFields(content, request, validFrom);
        content.setAuthorId(SecurityUtils.getUserId());
        content.setStatus(ContentStatus.DRAFT.getCode());
        this.save(content);

        replaceAttachments(content.getContentId(), request.getAttachments());
        return content.getContentId();
    }

    @Override
    public AdminContentDetailVO detail(Long contentId) {
        HealthContent content = requireContent(contentId);
        String partName = resolvePartName(content.getTargetPartCode());
        return AdminContentDetailVO.builder()
                .contentId(content.getContentId())
                .title(content.getTitle())
                .contentType(content.getContentType())
                .contentTypeName(ContentType.fromCode(content.getContentType()).getName())
                .targetPartCode(content.getTargetPartCode())
                .targetPartName(partName)
                .targetGroup(content.getTargetGroup())
                .summary(content.getSummary())
                .contentBody(content.getContentBody())
                .status(content.getStatus())
                .statusName(ContentStatus.fromCode(content.getStatus()).getName())
                .authorId(content.getAuthorId())
                .reviewerId(content.getReviewerId())
                .reviewRemark(content.getReviewRemark())
                .validFrom(content.getValidFrom())
                .validTo(content.getValidTo())
                .publishedAt(content.getPublishedAt())
                .createdAt(content.getCreatedAt())
                .updatedAt(content.getUpdatedAt())
                .attachments(buildAttachmentVOs(contentId))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContent(Long contentId, HealthContentSaveRequest request) {
        HealthContent content = requireContent(contentId);
        if (!ContentStatus.DRAFT.getCode().equals(content.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT,
                    "仅草稿状态的内容可编辑，当前状态：" + ContentStatus.fromCode(content.getStatus()).getName());
        }
        requirePartExists(request.getTargetPartCode());
        LocalDateTime validFrom = request.getValidFrom() != null ? request.getValidFrom() : LocalDateTime.now();
        validatePeriod(validFrom, request.getValidTo());

        applyEditableFields(content, request, validFrom);
        this.updateById(content);
        // 重新编辑视为处理上一轮驳回意见，清空历史审核标记（审核动作会重新写入）；
        // MyBatis-Plus updateById 默认忽略 null 字段，需显式 SET NULL
        this.update(new LambdaUpdateWrapper<HealthContent>()
                .eq(HealthContent::getContentId, contentId)
                .set(HealthContent::getReviewerId, null)
                .set(HealthContent::getReviewRemark, null));

        replaceAttachments(contentId, request.getAttachments());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long contentId, ContentAuditRequest request) {
        HealthContent content = requireContent(contentId);
        String decision = request.getDecision();
        String currentStatus = content.getStatus();
        Long reviewerId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();

        switch (decision) {
            case "APPROVE" -> {
                if (!ContentStatus.DRAFT.getCode().equals(currentStatus)
                        && !ContentStatus.PENDING_REVIEW.getCode().equals(currentStatus)) {
                    throw new BusinessException(ResultCode.STATE_CONFLICT,
                            "仅草稿或待审核内容可审核通过，当前状态：" + currentStatus);
                }
                content.setStatus(ContentStatus.PUBLISHED.getCode());
                content.setPublishedAt(now);
                // 历史数据 validFrom 兜底
                if (content.getValidFrom() == null) {
                    content.setValidFrom(now);
                }
                content.setReviewerId(reviewerId);
                if (StringUtils.hasText(request.getComment())) {
                    content.setReviewRemark(request.getComment());
                }
            }
            case "REJECT" -> {
                if (!ContentStatus.DRAFT.getCode().equals(currentStatus)
                        && !ContentStatus.PENDING_REVIEW.getCode().equals(currentStatus)) {
                    throw new BusinessException(ResultCode.STATE_CONFLICT,
                            "仅草稿或待审核内容可驳回，当前状态：" + currentStatus);
                }
                if (!StringUtils.hasText(request.getComment())) {
                    throw new BusinessException(ResultCode.VALIDATION_ERROR, "驳回必须填写理由");
                }
                content.setStatus(ContentStatus.DRAFT.getCode());
                content.setReviewerId(reviewerId);
                content.setReviewRemark(request.getComment());
            }
            case "OFFLINE" -> {
                if (!ContentStatus.PUBLISHED.getCode().equals(currentStatus)) {
                    throw new BusinessException(ResultCode.STATE_CONFLICT,
                            "仅已发布内容可下架，当前状态：" + currentStatus);
                }
                content.setStatus(ContentStatus.OFFLINE.getCode());
                content.setReviewerId(reviewerId);
                if (StringUtils.hasText(request.getComment())) {
                    content.setReviewRemark(request.getComment());
                }
            }
            default -> throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "不支持的审核动作：" + decision);
        }
        this.updateById(content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContent(Long contentId) {
        HealthContent content = requireContent(contentId);
        // 已发布内容禁止直接删除，必须先下架（保护线上患者侧可见数据）；
        // 草稿、待审核（撤回审核）、已下架内容均可删除
        if (ContentStatus.PUBLISHED.getCode().equals(content.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT,
                    "已发布内容请先下架再删除，当前状态：" + content.getStatus());
        }
        // 附件关联物理删除（file_object 保留，文件可能被其他内容复用或独立管理）
        contentAttachmentMapper.delete(new LambdaQueryWrapper<ContentAttachment>()
                .eq(ContentAttachment::getContentId, contentId));
        // 收藏关联物理删除（外键 fk_collection_content 无 ON DELETE 级联，须先清理）
        contentCollectionMapper.delete(new LambdaQueryWrapper<ContentCollection>()
                .eq(ContentCollection::getContentId, contentId));
        this.removeById(contentId);
    }

    // ==================== 内部辅助 ====================

    private void applyEditableFields(HealthContent content, HealthContentSaveRequest request,
                                     LocalDateTime validFrom) {
        content.setTitle(request.getTitle());
        content.setContentType(request.getContentType().getCode());
        content.setTargetPartCode(request.getTargetPartCode());
        content.setTargetGroup(request.getTargetGroup());
        content.setSummary(request.getSummary());
        content.setContentBody(request.getContentBody());
        content.setValidFrom(validFrom);
        content.setValidTo(request.getValidTo());
    }

    /**
     * 附件全量替换：校验文件存在/未删/扫描通过 + 请求内 fileId 不重复 → 物理删旧关联 → 插新关联。
     */
    private void replaceAttachments(Long contentId, List<HealthContentSaveRequest.AttachmentPayload> attachments) {
        contentAttachmentMapper.delete(new LambdaQueryWrapper<ContentAttachment>()
                .eq(ContentAttachment::getContentId, contentId));
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        // 请求内 fileId 去重校验（DB uk_content_file 兜底，提前给出可读错误）
        Set<Long> seenFileIds = new HashSet<>();
        for (HealthContentSaveRequest.AttachmentPayload payload : attachments) {
            if (!seenFileIds.add(payload.getFileId())) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR,
                        "附件文件重复引用，fileId=" + payload.getFileId());
            }
        }

        List<Long> fileIds = attachments.stream()
                .map(HealthContentSaveRequest.AttachmentPayload::getFileId)
                .toList();
        Map<Long, FileObject> fileMap = fileObjectMapper.selectBatchIds(fileIds).stream()
                .filter(f -> f.getDeletedAt() == null)
                .collect(Collectors.toMap(FileObject::getFileId, Function.identity()));

        List<ContentAttachment> rows = new ArrayList<>(attachments.size());
        int autoSort = 0;
        for (HealthContentSaveRequest.AttachmentPayload payload : attachments) {
            FileObject file = fileMap.get(payload.getFileId());
            if (file == null) {
                throw new BusinessException(ResultCode.FILE_NOT_FOUND,
                        "附件文件不存在或已删除：" + payload.getFileId());
            }
            if (!ScanStatus.PASSED.getCode().equals(file.getScanStatus())) {
                throw new BusinessException(ResultCode.FILE_SCAN_FAILED,
                        "附件文件未通过安全校验，不能关联内容：" + payload.getFileId());
            }
            ContentAttachment row = new ContentAttachment();
            row.setContentId(contentId);
            row.setFileId(payload.getFileId());
            row.setAttachmentType(payload.getAttachmentType().getCode());
            row.setSortNo(payload.getSortNo() != null ? payload.getSortNo() : autoSort);
            rows.add(row);
            autoSort++;
        }
        rows.forEach(contentAttachmentMapper::insert);
    }

    /**
     * 组装附件视图：批量查文件元数据，逐条生成预签名下载地址（签名必须逐条生成）。
     */
    private List<ContentAttachmentVO> buildAttachmentVOs(Long contentId) {
        List<ContentAttachment> rows = contentAttachmentMapper.selectList(
                new LambdaQueryWrapper<ContentAttachment>()
                        .eq(ContentAttachment::getContentId, contentId)
                        .orderByAsc(ContentAttachment::getSortNo)
                        .orderByAsc(ContentAttachment::getContentAttachmentId));
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, FileObject> fileMap = fileObjectMapper.selectBatchIds(
                        rows.stream().map(ContentAttachment::getFileId).toList()).stream()
                .filter(f -> f.getDeletedAt() == null)
                .collect(Collectors.toMap(FileObject::getFileId, Function.identity()));

        MinioFileService minio = minioFileServiceProvider.getIfAvailable();
        Integer expireSec = minioProperties.getPresignExpireSec();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expireSec);

        List<ContentAttachmentVO> result = new ArrayList<>(rows.size());
        for (ContentAttachment row : rows) {
            FileObject file = fileMap.get(row.getFileId());
            if (file == null) {
                continue;
            }
            AttachmentType type = AttachmentType.fromCode(row.getAttachmentType());
            ContentAttachmentVO.ContentAttachmentVOBuilder builder = ContentAttachmentVO.builder()
                    .contentAttachmentId(row.getContentAttachmentId())
                    .fileId(file.getFileId())
                    .attachmentType(type.getCode())
                    .attachmentTypeName(type.getName())
                    .sortNo(row.getSortNo())
                    .fileName(file.getFileName())
                    .contentType(file.getContentType())
                    .fileSize(file.getFileSize())
                    .urlExpiresAt(expiresAt);
            if (minio != null) {
                try {
                    builder.url(minio.presignedGetUrl(file.getBucketName(), file.getObjectKey()));
                } catch (IllegalStateException e) {
                    // 单条地址失败不阻断整个详情，url 留空由前端提示稍后重试
                    builder.url(null);
                }
            }
            result.add(builder.build());
        }
        return result;
    }

    private HealthContent requireContent(Long contentId) {
        HealthContent content = this.getById(contentId);
        if (content == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "内容不存在：" + contentId);
        }
        return content;
    }

    private void requirePartExists(String targetPartCode) {
        if (!StringUtils.hasText(targetPartCode)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "关联康复部位不能为空");
        }
        RehabPartDict dict = rehabPartDictMapper.selectById(targetPartCode);
        if (dict == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "关联康复部位编码不存在：" + targetPartCode);
        }
    }

    private void validatePeriod(LocalDateTime validFrom, LocalDateTime validTo) {
        if (validTo != null && validTo.isBefore(validFrom)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "失效时间不能早于生效时间");
        }
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

    private AdminContentVO toListVO(HealthContent c, Map<String, String> partNameMap) {
        ContentStatus status = ContentStatus.fromCode(c.getStatus());
        return AdminContentVO.builder()
                .contentId(c.getContentId())
                .title(c.getTitle())
                .contentType(c.getContentType())
                .contentTypeName(ContentType.fromCode(c.getContentType()).getName())
                .targetPartCode(c.getTargetPartCode())
                .targetPartName(partNameMap.getOrDefault(c.getTargetPartCode(), c.getTargetPartCode()))
                .targetGroup(c.getTargetGroup())
                .summary(c.getSummary())
                .status(status.getCode())
                .statusName(status.getName())
                .hasReviewRemark(StringUtils.hasText(c.getReviewRemark()))
                .reviewRemark(c.getReviewRemark())
                .validFrom(c.getValidFrom())
                .validTo(c.getValidTo())
                .publishedAt(c.getPublishedAt())
                .authorId(c.getAuthorId())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
