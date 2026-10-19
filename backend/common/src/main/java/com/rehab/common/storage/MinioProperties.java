package com.rehab.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 配置属性绑定类（前缀 {@code minio}）。
 *
 * <p>本类只负责承接配置，<b>不在代码里写任何默认值</b>；
 * 所有取值统一来自配置文件：公共默认在 application.yml 的 {@code minio:} 段，
 * 各环境差异在 application-dev.yml / application-prod.yml 覆盖，避免一份配置两处维护。</p>
 *
 * <p>绑定示例（yml 松散绑定，kebab-case 自动映射到驼峰字段）：</p>
 * <pre>
 * minio:
 *   enabled: true
 *   endpoint: http://192.168.10.102:9000
 *   access-key: minioadmin
 *   secret-key: minioadmin
 *   bucket-name: rehab
 *   presign-expire-sec: 600
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /** 是否启用 MinIO 客户端（false 时不创建 MinioClient Bean） */
    private Boolean enabled;

    /** 服务地址，如 http://192.168.10.102:9000 */
    private String endpoint;

    /** Access Key */
    private String accessKey;

    /** Secret Key */
    private String secretKey;

    /** 默认私有桶（对应配置键 minio.bucket-name） */
    private String bucketName;

    /** 预签名 URL 有效期（秒） */
    private Integer presignExpireSec;
}
