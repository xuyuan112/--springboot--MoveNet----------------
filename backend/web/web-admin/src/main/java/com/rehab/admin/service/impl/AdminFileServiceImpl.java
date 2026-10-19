package com.rehab.admin.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rehab.admin.service.AdminFileService;
import com.rehab.admin.service.file.FileUploadPolicy;
import com.rehab.admin.vo.FilePresignVO;
import com.rehab.admin.vo.FileUrlVO;
import com.rehab.model.mapper.action.ActionTemplateSampleMapper;
import com.rehab.model.mapper.consultation.ConsultationAttachmentMapper;
import com.rehab.model.mapper.content.ContentAttachmentMapper;
import com.rehab.model.mapper.system.FileObjectMapper;
import com.rehab.common.constant.RedisKeyConstants;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.common.storage.MinioFileService;
import com.rehab.common.storage.MinioProperties;
import com.rehab.model.dto.FileCompleteRequest;
import com.rehab.model.dto.FilePresignRequest;
import com.rehab.model.entity.ActionTemplateSample;
import com.rehab.model.entity.ConsultationAttachment;
import com.rehab.model.entity.ContentAttachment;
import com.rehab.model.entity.FileObject;
import com.rehab.model.enums.ScanStatus;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 管理端 - MinIO 文件生命周期服务实现。
 *
 * <p>直传流程：presign（白名单 + Redis 会话 + PUT 签名）→ 浏览器直传 MinIO →
 * complete（stat 校验大小/MIME + 流式 SHA-256 + 落库，会话一次性消费）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFileServiceImpl implements AdminFileService {

    private final ObjectProvider<MinioFileService> minioFileServiceProvider;
    private final MinioProperties minioProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final FileObjectMapper fileObjectMapper;
    private final ContentAttachmentMapper contentAttachmentMapper;
    private final ActionTemplateSampleMapper actionTemplateSampleMapper;
    private final ConsultationAttachmentMapper consultationAttachmentMapper;

    /** 文件名扩展名白字符提取（防 objectKey 注入） */
    private static final Pattern EXT_PATTERN = Pattern.compile("^([A-Za-z0-9]{1,10})$");

    /** 流式哈希读取缓冲区（8KB，不堆存整文件） */
    private static final int BUFFER_SIZE = 8192;

    /** Redis 中保存的直传会话元数据 */
    @lombok.Data
    private static class UploadSession {
        private String fileName;
        private String contentType;
        private Long fileSize;
        private String purpose;
        private String bucket;
        private String objectKey;
        private Long userId;
    }

    @Override
    public FilePresignVO presign(FilePresignRequest request) {
        // 1. 白名单（类型 / 大小）
        FileUploadPolicy.validate(request.getPurpose(), request.getContentType(), request.getFileSize());

        MinioFileService minio = requireMinio();
        String bucket = minioProperties.getBucketName();
        // 2. 懒初始化桶（MinIO 不可用时返回 424，业务数据无损）
        try {
            minio.ensureBucket(bucket);
        } catch (IllegalStateException e) {
            throw new BusinessException(ResultCode.STORAGE_UNAVAILABLE, "文件存储不可用：" + e.getMessage());
        }

        // 3. 生成对象键：用途前缀 + 无横线 UUID + 净化扩展名
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        String objectKey = FileUploadPolicy.keyPrefixOf(request.getPurpose()) + uploadId + extractExt(request.getFileName());

        // 4. 预签名 PUT
        Integer expireSec = minioProperties.getPresignExpireSec();
        String uploadUrl;
        try {
            uploadUrl = minio.presignedPutUrl(bucket, objectKey, expireSec);
        } catch (IllegalStateException e) {
            throw new BusinessException(ResultCode.STORAGE_UNAVAILABLE, "文件上传地址生成失败：" + e.getMessage());
        }

        // 5. 会话写 Redis（TTL 与签名一致）
        UploadSession session = new UploadSession();
        session.setFileName(request.getFileName());
        session.setContentType(request.getContentType());
        session.setFileSize(request.getFileSize());
        session.setPurpose(request.getPurpose().getCode());
        session.setBucket(bucket);
        session.setObjectKey(objectKey);
        session.setUserId(SecurityUtils.getUserId());
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.FILE_UPLOAD + uploadId,
                JSONUtil.toJsonStr(session),
                java.time.Duration.ofSeconds(expireSec));

        FilePresignVO vo = new FilePresignVO();
        vo.setUploadId(uploadId);
        vo.setUploadUrl(uploadUrl);
        vo.setMethod("PUT");
        vo.setHeaders(Map.of("Content-Type", request.getContentType()));
        vo.setExpiresInSec(expireSec);
        return vo;
    }

    @Override
    public Long complete(String uploadId, FileCompleteRequest request) {
        // 1. 取一次性会话（进入确认即消费，失败须重新 presign，防止重放）
        String sessionKey = RedisKeyConstants.FILE_UPLOAD + uploadId;
        String json = stringRedisTemplate.opsForValue().get(sessionKey);
        if (json == null) {
            throw new BusinessException(ResultCode.FILE_URL_EXPIRED, "上传会话不存在或已过期，请重新申请上传地址");
        }
        stringRedisTemplate.delete(sessionKey);
        UploadSession session = JSONUtil.toBean(json, UploadSession.class);

        MinioFileService minio = requireMinio();

        // 2. stat：对象存在性 / 大小 / MIME
        StatObjectResponse stat;
        try {
            stat = minio.statObject(session.getBucket(), session.getObjectKey());
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new BusinessException(ResultCode.FILE_NOT_FOUND, "尚未检测到已上传的对象，请先完成文件直传");
            }
            log.warn("MinIO statObject 异常 key={}", session.getObjectKey(), e);
            throw new BusinessException(ResultCode.STORAGE_UNAVAILABLE, "文件存储校验失败");
        } catch (Exception e) {
            log.warn("MinIO statObject 异常 key={}", session.getObjectKey(), e);
            throw new BusinessException(ResultCode.STORAGE_UNAVAILABLE, "文件存储校验失败");
        }

        if (stat.size() != session.getFileSize()) {
            // 与申报大小不符：清理错误对象
            safeRemove(minio, session);
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    String.format("实际上传文件大小 %d 与申报大小 %d 不一致", stat.size(), session.getFileSize()));
        }
        String actualContentType = stat.contentType();
        if (actualContentType == null
                || !actualContentType.trim().equalsIgnoreCase(session.getContentType())) {
            safeRemove(minio, session);
            throw new BusinessException(ResultCode.UNSUPPORTED_MEDIA_TYPE,
                    "实际上传文件类型 " + actualContentType + " 与申报类型 " + session.getContentType() + " 不一致");
        }

        // 3. 流式 SHA-256（内网从 MinIO 拉取，不落盘、不堆存）
        String expectedSha = request.getSha256().toLowerCase();
        String actualSha;
        try (InputStream objectStream = minio.getObject(session.getBucket(), session.getObjectKey());
             DigestInputStream dis = new DigestInputStream(objectStream, MessageDigest.getInstance("SHA-256"))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (dis.read(buffer) != -1) {
                // 读取即驱动摘要更新
            }
            actualSha = HexFormat.of().formatHex(dis.getMessageDigest().digest());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("MinIO 对象读取/摘要失败 key={}", session.getObjectKey(), e);
            throw new BusinessException(ResultCode.STORAGE_UNAVAILABLE, "文件哈希校验失败，请稍后重试");
        }

        if (!expectedSha.equals(actualSha)) {
            // 哈希不符：传输损坏或文件被替换，清理脏对象
            safeRemove(minio, session);
            throw new BusinessException(ResultCode.FILE_HASH_MISMATCH,
                    "文件 SHA-256 校验不一致，已拒绝入库");
        }

        // 4. 落库（首版无第三方杀毒，白名单 + 哈希通过即视为 PASSED）
        FileObject file = new FileObject();
        file.setBucketName(session.getBucket());
        file.setObjectKey(session.getObjectKey());
        file.setFileName(session.getFileName());
        file.setContentType(session.getContentType());
        file.setFileSize(session.getFileSize());
        file.setSha256(actualSha);
        file.setPurpose(session.getPurpose());
        file.setScanStatus(ScanStatus.PASSED.getCode());
        file.setCreatedBy(session.getUserId());
        file.setCreatedAt(LocalDateTime.now());
        fileObjectMapper.insert(file);
        return file.getFileId();
    }

    @Override
    public FileUrlVO getUrl(Long fileId) {
        FileObject file = requireActiveFile(fileId);
        MinioFileService minio = requireMinio();
        String url;
        try {
            url = minio.presignedGetUrl(file.getBucketName(), file.getObjectKey());
        } catch (IllegalStateException e) {
            throw new BusinessException(ResultCode.STORAGE_UNAVAILABLE, "文件访问地址生成失败：" + e.getMessage());
        }

        Integer expireSec = minioProperties.getPresignExpireSec();
        FileUrlVO vo = new FileUrlVO();
        vo.setFileId(file.getFileId());
        vo.setFileName(file.getFileName());
        vo.setContentType(file.getContentType());
        vo.setFileSize(file.getFileSize());
        vo.setUrl(url);
        vo.setUrlExpiresAt(LocalDateTime.now().plusSeconds(expireSec));
        return vo;
    }

    @Override
    public void delete(Long fileId) {
        FileObject file = requireActiveFile(fileId);

        // 引用检查：内容附件 / 动作模板样本 / 咨询消息附件
        Long contentRef = contentAttachmentMapper.selectCount(
                new LambdaQueryWrapper<ContentAttachment>().eq(ContentAttachment::getFileId, fileId));
        if (contentRef != null && contentRef > 0) {
            throw new BusinessException(ResultCode.FILE_IN_USE,
                    "文件已被 " + contentRef + " 条公共内容附件引用，请先移除引用");
        }
        Long sampleRef = actionTemplateSampleMapper.selectCount(
                new LambdaQueryWrapper<ActionTemplateSample>().eq(ActionTemplateSample::getFileId, fileId));
        if (sampleRef != null && sampleRef > 0) {
            throw new BusinessException(ResultCode.FILE_IN_USE,
                    "文件已被 " + sampleRef + " 条动作模板样本引用，不可删除");
        }
        Long messageRef = consultationAttachmentMapper.selectCount(
                new LambdaQueryWrapper<ConsultationAttachment>().eq(ConsultationAttachment::getFileId, fileId));
        if (messageRef != null && messageRef > 0) {
            throw new BusinessException(ResultCode.FILE_IN_USE,
                    "文件已被 " + messageRef + " 条咨询消息附件引用，不可删除");
        }

        // 先删对象再软删记录；对象删除失败整体中止（保留记录便于重试）
        try {
            requireMinio().removeObject(file.getBucketName(), file.getObjectKey());
        } catch (IllegalStateException e) {
            throw new BusinessException(ResultCode.STORAGE_UNAVAILABLE, "文件删除失败：" + e.getMessage());
        }
        FileObject update = new FileObject();
        update.setFileId(fileId);
        update.setDeletedAt(LocalDateTime.now());
        fileObjectMapper.updateById(update);
        log.info("文件已删除 fileId={}, key={}", fileId, file.getObjectKey());
    }

    // ==================== 内部辅助 ====================

    private MinioFileService requireMinio() {
        MinioFileService service = minioFileServiceProvider.getIfAvailable();
        if (service == null) {
            throw new BusinessException(ResultCode.STORAGE_UNAVAILABLE,
                    "文件存储未启用（minio.enabled=false），请联系管理员");
        }
        return service;
    }

    private FileObject requireActiveFile(Long fileId) {
        FileObject file = fileObjectMapper.selectOne(new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getFileId, fileId)
                .isNull(FileObject::getDeletedAt));
        if (file == null) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND, "文件不存在或已删除：" + fileId);
        }
        return file;
    }

    private void safeRemove(MinioFileService minio, UploadSession session) {
        try {
            minio.removeObject(session.getBucket(), session.getObjectKey());
        } catch (Exception ignored) {
            // 清理失败不影响错误响应；孤儿对象由后续对账清理
            log.warn("校验失败后清理 MinIO 异常对象未成功 key={}", session.getObjectKey());
        }
    }

    /** 提取并净化文件名扩展名（含点，小写）；无合法扩展名返回空串 */
    private String extractExt(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(dot + 1);
        Matcher matcher = EXT_PATTERN.matcher(ext);
        return matcher.matches() ? "." + ext.toLowerCase() : "";
    }
}
