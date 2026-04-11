package ww.wangnong.mqtt;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import ww.wangnong.bridge.TelemetryTranslator;
import ww.wangnong.config.RunMode;
import ww.wangnong.config.RunModeHolder;

@Service
public class PahoMqttService {

    private static final Logger log = LoggerFactory.getLogger(PahoMqttService.class);

    private final MqttProperties properties;
    private final RunMode runMode;
    private final MqttClientFactory clientFactory;

    private final Map<String, SubscriptionBinding> subscriptions = new ConcurrentHashMap<>();
    private static final AtomicInteger HANDLER_THREAD_SEQ = new AtomicInteger(0);
    private final ExecutorService handlerExecutor = Executors.newFixedThreadPool(4,
            r -> {
                Thread t = new Thread(r, "mqtt-handler-" + HANDLER_THREAD_SEQ.getAndIncrement());
                t.setDaemon(true);
                return t;
            });
    private final ExecutorService telemetryExecutor = Executors.newSingleThreadExecutor(
            r -> {
                Thread t = new Thread(r, "mqtt-telemetry-handler");
                t.setDaemon(true);
                return t;
            });
    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor(
            r -> {
                Thread t = new Thread(r, "mqtt-command-handler");
                t.setDaemon(true);
                return t;
            });

    private MqttAsyncClient client;
    private volatile ConnectionState state = ConnectionState.DISCONNECTED;
    private volatile String activeClientId;
    private volatile Instant lastConnectedAt;
    private volatile String lastError;

    @Autowired
    public PahoMqttService(MqttProperties properties, RunModeHolder runModeHolder) {
        this(
                properties,
                runModeHolder == null ? RunMode.MOCK : runModeHolder.getMode(),
                (serverUri, clientId) -> new MqttAsyncClient(serverUri, clientId, new MemoryPersistence()));
    }

    PahoMqttService(MqttProperties properties, MqttClientFactory clientFactory) {
        this(properties, RunMode.MOCK, clientFactory);
    }

    PahoMqttService(MqttProperties properties, RunMode runMode, MqttClientFactory clientFactory) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.runMode = runMode == null ? RunMode.MOCK : runMode;
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory cannot be null");
    }

    @PostConstruct
    public void connect() {
        if (!properties.isEnabled()) {
            state = ConnectionState.DISABLED;
            lastError = null;
            log.info("MQTT is disabled by configuration.");
            return;
        }

        state = ConnectionState.CONNECTING;
        lastError = null;
        try {
            String configuredClientId = properties.getClientId();
            String clientId = (configuredClientId == null || configuredClientId.isBlank())
                    ? MqttAsyncClient.generateClientId()
                    : configuredClientId;
            this.activeClientId = clientId;

            this.client = clientFactory.create(properties.getServerUri(), clientId);
            this.client.setCallback(buildCallback());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(properties.isCleanSession());
            options.setConnectionTimeout(properties.getConnectionTimeoutSeconds());
            options.setKeepAliveInterval(properties.getKeepAliveIntervalSeconds());
            options.setMaxReconnectDelay(10_000);
            options.setMaxInflight(50);

            if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
                options.setUserName(properties.getUsername());
            }
            if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
                options.setPassword(properties.getPassword().toCharArray());
            }

            client.connect(options).waitForCompletion();
            markConnected();
            log.info("MQTT connected: uri={}, clientId={}", properties.getServerUri(), clientId);
            resubscribeAll();
        } catch (MqttException e) {
            state = ConnectionState.FAILED;
            lastError = describeError(e);
            log.error("MQTT connection failed. HTTP server remains available.", e);
        }
    }

    public boolean isConnected() {
        return state == ConnectionState.CONNECTED && client != null && client.isConnected();
    }

    public LifecycleSnapshot lifecycleSnapshot() {
        String connectedAt = lastConnectedAt == null ? null : lastConnectedAt.toString();
        return new LifecycleSnapshot(
                state,
                isConnected(),
                properties.getServerUri(),
                activeClientId,
                connectedAt,
                lastError,
                subscriptions.size());
    }

    public void publish(String topic, String payload, Integer qos, boolean retained) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("MQTT is disabled.");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("payload cannot be blank");
        }
        if (!isConnected()) {
            log.warn("[MQTT] Publish skipped (not connected): topic={}", topic);
            return;
        }

        String targetTopic = (topic == null || topic.isBlank()) ? properties.getDefaultTopic() : topic;
        int normalizedQos = normalizeQos(qos != null ? qos : properties.getQos());

        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(normalizedQos);
            message.setRetained(retained);

            IMqttDeliveryToken token = client.publish(targetTopic, message);
            token.waitForCompletion(5_000);
            if (shouldLogOutboundToConsole()) {
                log.info(
                        "[MQTT][OUT] topic={}, qos={}, retained={}, payload={}",
                        targetTopic,
                        normalizedQos,
                        retained,
                        TelemetryTranslator.briefPayload(payload));
            } else {
                log.debug("MQTT message published: topic={}, qos={}, retained={}", targetTopic, normalizedQos,
                        retained);
            }
        } catch (MqttException e) {
            log.warn("[MQTT] Publish failed (will retry on reconnect): topic={}", targetTopic, e);
        }
    }

    /**
     * Non-blocking publish — fire-and-forget, does NOT call waitForCompletion().
     * Use for latency-sensitive forwarding (e.g., command bridge).
     */
    public void publishAsync(String topic, String payload, int qos, boolean retained) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("MQTT is disabled.");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("payload cannot be blank");
        }
        if (!isConnected()) {
            log.warn("[MQTT] PublishAsync skipped (not connected): topic={}", topic);
            return;
        }

        String targetTopic = (topic == null || topic.isBlank()) ? properties.getDefaultTopic() : topic;
        int normalizedQos = normalizeQos(qos);

        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(normalizedQos);
            message.setRetained(retained);

            client.publish(targetTopic, message);
            log.info("[MQTT][ASYNC-OUT] topic={}, qos={}, payload={}", targetTopic, normalizedQos,
                    TelemetryTranslator.briefPayload(payload));
        } catch (MqttException e) {
            log.warn("[MQTT] PublishAsync failed: topic={}", targetTopic, e);
        }
    }

    public void subscribe(String topicFilter, Consumer<InboundMessage> handler) {
        subscribe(topicFilter, properties.getQos(), handler);
    }

    public void subscribe(String topicFilter, Integer qos, Consumer<InboundMessage> handler) {
        if (topicFilter == null || topicFilter.isBlank()) {
            throw new IllegalArgumentException("topicFilter cannot be blank");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler cannot be null");
        }

        int normalizedQos = normalizeQos(qos != null ? qos : properties.getQos());
        subscriptions.put(topicFilter, new SubscriptionBinding(normalizedQos, handler));

        if (!isConnected()) {
            log.info("MQTT not connected. Subscription cached: topicFilter={}", topicFilter);
            return;
        }

        subscribeInternal(topicFilter, normalizedQos);
    }

    public void unsubscribe(String topicFilter) {
        if (topicFilter == null || topicFilter.isBlank()) {
            throw new IllegalArgumentException("topicFilter cannot be blank");
        }

        subscriptions.remove(topicFilter);
        if (!isConnected()) {
            return;
        }

        try {
            client.unsubscribe(topicFilter).waitForCompletion();
            log.info("MQTT unsubscribed: topicFilter={}", topicFilter);
        } catch (MqttException e) {
            throw new IllegalStateException("Failed to unsubscribe MQTT topic.", e);
        }
    }

    @PreDestroy
    public void disconnect() {
        if (client == null) {
            state = properties.isEnabled() ? ConnectionState.DISCONNECTED : ConnectionState.DISABLED;
            return;
        }

        telemetryExecutor.shutdown();
        commandExecutor.shutdown();
        handlerExecutor.shutdown();
        try {
            telemetryExecutor.awaitTermination(5, TimeUnit.SECONDS);
            commandExecutor.awaitTermination(5, TimeUnit.SECONDS);
            handlerExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        try {
            if (client.isConnected()) {
                client.disconnect().waitForCompletion();
            }
            client.close();
            state = ConnectionState.DISCONNECTED;
            log.info("MQTT disconnected.");
        } catch (MqttException e) {
            lastError = describeError(e);
            log.warn("Error while disconnecting MQTT client.", e);
        }
    }

    private MqttCallbackExtended buildCallback() {
        return new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                markConnected();
                log.info("MQTT connectComplete: reconnect={}, uri={}", reconnect, serverURI);
                resubscribeAll();
            }

            @Override
            public void connectionLost(Throwable cause) {
                state = ConnectionState.DISCONNECTED;
                lastError = describeError(cause);
                log.warn("MQTT connection lost.", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                dispatchMessage(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // no-op
            }
        };
    }

    private void subscribeInternal(String topicFilter, int qos) {
        try {
            client.subscribe(topicFilter, qos).waitForCompletion();
            log.info("MQTT subscribed: topicFilter={}, qos={}", topicFilter, qos);
        } catch (MqttException e) {
            throw new IllegalStateException("Failed to subscribe MQTT topic.", e);
        }
    }

    private void resubscribeAll() {
        if (!isConnected() || subscriptions.isEmpty()) {
            return;
        }

        subscriptions.forEach((topicFilter, binding) -> {
            try {
                client.subscribe(topicFilter, binding.qos()).waitForCompletion();
                log.info("MQTT re-subscribed: topicFilter={}, qos={}", topicFilter, binding.qos());
            } catch (MqttException e) {
                log.warn("Failed to re-subscribe MQTT topic: topicFilter={}", topicFilter, e);
            }
        });
    }

    private void dispatchMessage(String topic, MqttMessage message) {
        if (subscriptions.isEmpty()) {
            return;
        }

        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        InboundMessage inbound = new InboundMessage(topic, payload, message.getQos(), message.isRetained());

        subscriptions.forEach((topicFilter, binding) -> {
            if (!MqttTopic.isMatched(topicFilter, topic)) {
                return;
            }

            ExecutorService executor = selectExecutor(topicFilter, topic);
            executor.execute(() -> {
                try {
                    binding.handler().accept(inbound);
                } catch (Exception ex) {
                    log.warn("MQTT subscription handler failed: topicFilter={}, topic={}", topicFilter, topic, ex);
                }
            });
        });
    }

    private boolean isCommandTopic(String topicFilter, String topic) {
        return (topicFilter != null && topicFilter.endsWith("/command"))
                || (topic != null && topic.endsWith("/command"));
    }

    private boolean isTelemetryTopic(String topicFilter, String topic) {
        return (topicFilter != null && topicFilter.endsWith("/telemetry"))
                || (topic != null && topic.endsWith("/telemetry"));
    }

    private ExecutorService selectExecutor(String topicFilter, String topic) {
        if (isCommandTopic(topicFilter, topic)) {
            return commandExecutor;
        }
        if (isTelemetryTopic(topicFilter, topic)) {
            return telemetryExecutor;
        }
        return handlerExecutor;
    }

    private int normalizeQos(int qos) {
        if (qos < 0) {
            return 0;
        }
        if (qos > 2) {
            return 2;
        }
        return qos;
    }

    private void markConnected() {
        state = ConnectionState.CONNECTED;
        lastConnectedAt = Instant.now();
        lastError = null;
    }

    private boolean shouldLogOutboundToConsole() {
        return runMode == RunMode.HARDWARE;
    }

    private static String describeError(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getClass().getSimpleName() + ": " + message;
    }

    public record InboundMessage(String topic, String payload, int qos, boolean retained) {
    }

    public enum ConnectionState {
        DISABLED,
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        FAILED
    }

    public record LifecycleSnapshot(
            ConnectionState state,
            boolean connected,
            String serverUri,
            String clientId,
            String lastConnectedAt,
            String lastError,
            int cachedSubscriptionCount) {
    }

    @FunctionalInterface
    interface MqttClientFactory {
        MqttAsyncClient create(String serverUri, String clientId) throws MqttException;
    }

    private record SubscriptionBinding(int qos, Consumer<InboundMessage> handler) {
    }
}
