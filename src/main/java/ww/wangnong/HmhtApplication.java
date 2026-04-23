package ww.wangnong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Purpose: HMHT 课程版后端 Spring Boot 启动入口。
 * Responsibilities: 启用自动配置、配置属性扫描与调度能力，并装配 REST 与持久化应用上下文。
 * Flow: JVM 启动 main -> SpringApplication.run -> Spring 容器初始化。
 * Dependencies: Spring Boot 自动配置。
 * Input: 命令行参数、环境变量、application 配置。
 * Output: 已启动的 HMHT 课程版应用进程。
 * Side Effects: 创建完整应用上下文并触发启动种子数据和调度任务。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class HmhtApplication {

    public static void main(String[] args) {
        SpringApplication.run(HmhtApplication.class, args);
    }

}
