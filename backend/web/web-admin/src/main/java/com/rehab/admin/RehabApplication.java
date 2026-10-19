package com.rehab.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 平台唯一启动入口。
 *
 * <p>scanBasePackages 覆盖 com.rehab 全包，因此 web-app（患者/治疗师/公共接口）
 * 与 web-admin（管理端接口、定时任务）在同一进程内承载；
 * 如后续需要按端拆分微服务，将 web-app 独立出启动类即可。</p>
 *
 * <p>Mapper 统一扫描 {@code com.rehab.**.mapper}（当前全部位于 web-app 模块）。</p>
 *
 * <p>认证采用自研 JWT 过滤器（{@code JwtAuthenticationFilter}），不使用
 * UserDetailsService 体系，故排除 {@link UserDetailsServiceAutoConfiguration}，
 * 避免启动日志产生误导性的随机默认密码。</p>
 */
@SpringBootApplication(
        scanBasePackages = "com.rehab",
        exclude = UserDetailsServiceAutoConfiguration.class
)
@ConfigurationPropertiesScan(basePackages = "com.rehab.common")
@MapperScan("com.rehab.**.mapper")
@EnableScheduling
public class RehabApplication {

    public static void main(String[] args) {
        SpringApplication.run(RehabApplication.class, args);
    }
}
