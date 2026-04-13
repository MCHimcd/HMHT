package ww.wangnong.config;

import com.influxdb.v3.client.InfluxDBClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Purpose: 在启用 InfluxDB 时装配客户端 Bean。
 * Responsibilities: 根据结构化配置创建 InfluxDBClient，供时序写入和查询服务注入使用。
 * Flow: hmht.influxdb.enabled=true -> Spring 创建 InfluxDBClient Bean ->
 * 相关服务注入使用。
 * Dependencies: InfluxDbProperties、InfluxDB Java Client。
 * Input: InfluxDB URL、token、database 配置。
 * Output: 可复用的 InfluxDBClient Bean。
 */
@Configuration
@ConditionalOnProperty(prefix = "hmht.influxdb", name = "enabled", havingValue = "true")
public class InfluxDbConfig {

    @SuppressWarnings("null")
    @Bean
    public InfluxDBClient influxDBClient(InfluxDbProperties properties) {
        return InfluxDBClient.getInstance(
                properties.getUrl(),
                properties.getToken().toCharArray(),
                properties.getDatabase());
    }
}
