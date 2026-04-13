package ww.wangnong.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Purpose: 承载 MQTT 客户端的结构化配置。
 * Responsibilities: 暴露启用开关、Broker 地址、认证信息、QoS、默认主题与连接参数。
 * Flow: Spring 绑定 hmht.mqtt.* 配置 -> PahoMqttService 读取属性并创建客户端。
 * Dependencies: @ConfigurationProperties、PahoMqttService。
 * Input: application 配置与环境变量中的 MQTT 参数。
 * Output: 可注入的 MQTT 配置对象。
 */
@ConfigurationProperties(prefix = "hmht.mqtt")
public class MqttProperties {

    private boolean enabled = true;

    private String serverUri = "tcp://broker.emqx.io:1883";

    private String clientId = "hmht-server";

    private String username;

    private String password;

    private boolean cleanSession = true;

    private int connectionTimeoutSeconds = 10;

    private int keepAliveIntervalSeconds = 30;

    private String defaultTopic = "hmht/default";

    private int qos = 1;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerUri() {
        return serverUri;
    }

    public void setServerUri(String serverUri) {
        this.serverUri = serverUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public int getConnectionTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }

    public void setConnectionTimeoutSeconds(int connectionTimeoutSeconds) {
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
    }

    public int getKeepAliveIntervalSeconds() {
        return keepAliveIntervalSeconds;
    }

    public void setKeepAliveIntervalSeconds(int keepAliveIntervalSeconds) {
        this.keepAliveIntervalSeconds = keepAliveIntervalSeconds;
    }

    public String getDefaultTopic() {
        return defaultTopic;
    }

    public void setDefaultTopic(String defaultTopic) {
        this.defaultTopic = defaultTopic;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }
}
