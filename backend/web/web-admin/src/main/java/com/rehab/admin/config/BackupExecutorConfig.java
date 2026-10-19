package com.rehab.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 备份任务线程池：异步执行 JDBC 逻辑备份，不占用 Web 请求线程。
 */
@Configuration
public class BackupExecutorConfig {

    /** 备份任务 Executor Bean 名 */
    public static final String BACKUP_EXECUTOR = "backupTaskExecutor";

    @Bean(name = BACKUP_EXECUTOR)
    public Executor backupTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        // 队列容量 1：备份锁之外再兜底一层，多余任务快速失败
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("backup-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(300);
        executor.initialize();
        return executor;
    }
}
