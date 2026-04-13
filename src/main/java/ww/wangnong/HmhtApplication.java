package ww.wangnong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Purpose: HMHT 后端 Spring Boot 启动入口。
 * Responsibilities: 启用自动配置、配置属性扫描与调度能力，并装配整个 REST、MQTT、持久化应用上下文。
 * Flow: JVM 启动 main -> SpringApplication.run -> Spring 容器初始化 -> 条件 Bean
 * 按运行模式激活。
 * Dependencies: Spring Boot 自动配置、@ConfigurationProperties 配置类、各运行模式条件装配。
 * Input: 命令行参数、环境变量、application 配置。
 * Output: 已启动的 HMHT 应用进程与可用的 Web/MQTT/Scheduled Bean。
 * Side Effects: 创建完整应用上下文并触发 @PostConstruct、调度任务和监听组件启动。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class HmhtApplication {

    public static void main(String[] args) {
        SpringApplication.run(HmhtApplication.class, args);
    }

}
