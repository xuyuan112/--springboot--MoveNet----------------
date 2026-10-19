package com.rehab.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数据库备份配置（rehab.backup.*）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "rehab.backup")
public class BackupProperties {

    /** 备份文件本地存储目录（不存在自动创建） */
    private String dir = "data/backups";
}
