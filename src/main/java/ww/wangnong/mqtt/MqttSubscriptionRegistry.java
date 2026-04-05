package ww.wangnong.mqtt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

@Component
public class MqttSubscriptionRegistry {

    private final Map<String, SubscriptionDefinition> definitions = new LinkedHashMap<>();

    public synchronized void register(String topicFilter, Integer qos, Consumer<PahoMqttService.InboundMessage> handler) {
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
