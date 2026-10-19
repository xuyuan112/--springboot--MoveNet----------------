package com.rehab.common.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件服务：业务表只存 object_key，读取时由后端生成短时预签名 URL，
 * 禁止下发永久公开 URL（需求 2.2 对象级权限规则）。
 *
 * <p>桶前缀规划：{@code avatar/} 头像、{@code action-demo/} 动作演示、
 * {@code content/} 公共内容、{@code message/} 咨询附件。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "true")
public class MinioFileService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 生成对象的短时下载 URL（默认有效期取配置值）。
     */
    public String presignedGetUrl(String bucket, String objectKey) {
        return presignedGetUrl(bucket, objectKey, minioProperties.getPresignExpireSec());
    }

    /**
     * 生成对象的短时下载 URL。
     */
    public String presignedGetUrl(String bucket, String objectKey, Integer expireSec) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(expireSec, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            log.error("生成 MinIO 预签名 URL 失败 bucket={}, objectKey={}", bucket, objectKey, e);
            throw new IllegalStateException("文件访问地址生成失败", e);
        }
    }

    /**
     * 生成对象的短时上传 URL（前端直传场景，后续迭代使用）。
     */
    public String presignedPutUrl(String bucket, String objectKey, Integer expireSec) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(expireSec, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            log.error("生成 MinIO 上传 URL 失败 bucket={}, objectKey={}", bucket, objectKey, e);
            throw new IllegalStateException("文件上传地址生成失败", e);
        }
    }

    /**
     * 查询对象元数据（大小、Content-Type 等）；对象不存在时抛出
     * {@link ErrorResponseException}（NoSuchKey），由调用方区分处理。
     */
    public StatObjectResponse statObject(String bucket, String objectKey) throws Exception {
        return minioClient.statObject(StatObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build());
    }

    /**
     * 获取对象输入流（调用方负责关闭，用于流式哈希校验等场景）。
     */
    public InputStream getObject(String bucket, String objectKey) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build());
    }

    /**
     * 删除对象（对象不存在时 MinIO 视为成功，幂等）。
     */
    public void removeObject(String bucket, String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("删除 MinIO 对象失败 bucket={}, objectKey={}", bucket, objectKey, e);
            throw new IllegalStateException("文件删除失败", e);
        }
    }

    /**
     * 确保桶存在：不存在则创建（默认私有桶）。用于首次文件操作时的懒初始化，
     * 避免应用启动阶段强依赖 MinIO 可用。
     *
     * @return 桶已存在或创建成功返回 true
     */
    public boolean ensureBucket(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO 桶不存在，已自动创建私有桶：{}", bucket);
            }
            return true;
        } catch (Exception e) {
            log.error("初始化 MinIO 桶失败 bucket={}", bucket, e);
            throw new IllegalStateException("文件存储初始化失败", e);
        }
    }
}
