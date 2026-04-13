package ww.wangnong.mqtt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

/**
 * Purpose: 统一收集桥接层声明的 MQTT 订阅定义。
 * Responsibilities: 暂存 topicFilter/qos/handler 三元组，并在 MQTT 客户端可用后集中绑定。
 * Flow: 组件调用 register 声明订阅 -> snapshot/bindAll 遍历定义 -> PahoMqttService 执行实际
 * subscribe。
 * Dependencies: PahoMqttService。
 * Input: topicFilter、QoS、消息处理函数。
 * Output: 可重放的订阅定义集合。
 */
@Component
public class MqttSubscriptionRegistry {

    private final Map<String, SubscriptionDefinition> definitions = new LinkedHashMap<>();

    public synchronized void register(String topicFilter, Integer qos,
            Consumer<PahoMqttService.InboundMessage> handler) {
        if (topicFilter == null || topicFilter.isBlank()) {
            throw new IllegalArgumentException("topicFilter cannot be blank");
        }
        Objects.requireNonNull(handler, "handler cannot be null");

        definitions.put(topicFilter, new SubscriptionDefinition(topicFilter, qos, handler));
    }

    public synchronized List<SubscriptionDefinition> snapshot() {
        return List.copyOf(new ArrayList<>(definitions.values()));
    }

    public void bindAll(PahoMqttService mqttService) {
        Objects.requireNonNull(mqttService, "mqttService cannot be null");
        List<SubscriptionDefinition> snapshot = snapshot();
        for (SubscriptionDefinition definition : snapshot) {
            mqttService.subscribe(definition.topicFilter(), definition.qos(), definition.handler());
        }
    }

    public record SubscriptionDefinition(
            String topicFilter,
            Integer qos,
            Consumer<PahoMqttService.InboundMessage> handler) {
    }
}
