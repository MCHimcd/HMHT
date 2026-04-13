package ww.wangnong.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Purpose: 承载 InfluxDB 集成的结构化配置。
 * Responsibilities: 暴露启用开关、连接地址、数据库名和访问 token，避免散落的 @Value 配置。
 * Flow: Spring Boot 绑定 hmht.influxdb.* 配置 -> InfluxDbConfig/相关服务读取属性。
 * Dependencies: @ConfigurationProperties 绑定机制。
 * Input: application.properties 或环境变量中的 InfluxDB 配置。
 * Output: 可注入的 InfluxDbProperties 配置对象。
 */
@ConfigurationProperties(prefix = "hmht.influxdb")
public class InfluxDbProperties {

    private boolean enabled = true;

    private String url = "http://localhost:8181";

    private String database = "wangnong";

    private String token = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
