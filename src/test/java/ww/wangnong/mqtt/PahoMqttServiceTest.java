package ww.wangnong.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import ww.wangnong.config.RunMode;

@ExtendWith({ MockitoExtension.class, OutputCaptureExtension.class })
class PahoMqttServiceTest {

    @Mock
    private MqttAsyncClient mqttClient;

    @Mock
    private IMqttToken connectToken;

    @Mock
    private IMqttToken subscribeToken;

    @Mock
    private IMqttDeliveryToken deliveryToken;

    private MqttProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MqttProperties();
        properties.setEnabled(true);
        properties.setServerUri("tcp://localhost:1883");
        properties.setClientId("test-client");
        properties.setQos(1);
    }

    @Test
    void connect_disabled_setsSnapshotDisabledAndSkipsClientCreation() {
        properties.setEnabled(false);
        AtomicBoolean clientCreated = new AtomicBoolean(false);
        PahoMqttService service = new PahoMqttService(properties, (uri, clientId) -> {
            clientCreated.set(true);
            return mqttClient;
        });

        service.connect();

        assertThat(clientCreated).isFalse();
        assertThat(service.lifecycleSnapshot().state()).isEqualTo(PahoMqttService.ConnectionState.DISABLED);
        assertThat(service.lifecycleSnapshot().connected()).isFalse();
    }

    @Test
    void subscribe_notConnected_cachesSubscriptionWithoutCallingClient() throws Exception {
        PahoMqttService service = new PahoMqttService(properties, (uri, clientId) -> mqttClient);

        service.subscribe("/hardware/device/+/telemetry", 1, msg -> {
        });

        assertThat(service.lifecycleSnapshot().cachedSubscriptionCount()).isEqualTo(1);
        verify(mqttClient, never()).subscribe(anyString(), anyInt());
    }

    @Test
    void connect_withCachedSubscriptions_resubscribesAndMarksConnected() throws Exception {
        when(mqttClient.connect(any(MqttConnectOptions.class))).thenReturn(connectToken);
        when(mqttClient.isConnected()).thenReturn(true);
        when(mqttClient.subscribe(anyString(), anyInt())).thenReturn(subscribeToken);

        PahoMqttService service = new PahoMqttService(properties, (uri, clientId) -> mqttClient);
        service.subscribe("/hardware/device/+/telemetry", 1, msg -> {
        });

        service.connect();

        verify(mqttClient).subscribe(eq("/hardware/device/+/telemetry"), eq(1));
        assertThat(service.lifecycleSnapshot().state()).isEqualTo(PahoMqttService.ConnectionState.CONNECTED);
        assertThat(service.lifecycleSnapshot().connected()).isTrue();
    }

    @Test
    void connect_failure_marksSnapshotFailed() throws Exception {
        when(mqttClient.connect(any(MqttConnectOptions.class))).thenThrow(new MqttException(1));

        PahoMqttService service = new PahoMqttService(properties, (uri, clientId) -> mqttClient);

        service.connect();

        assertThat(service.lifecycleSnapshot().state()).isEqualTo(PahoMqttService.ConnectionState.FAILED);
        assertThat(service.lifecycleSnapshot().connected()).isFalse();
        assertThat(service.lifecycleSnapshot().lastError()).isNotBlank();
    }

    @Test
    void callback_connectionLost_marksDisconnectedAndStoresError() throws Exception {
        when(mqttClient.connect(any(MqttConnectOptions.class))).thenReturn(connectToken);
        when(mqttClient.isConnected()).thenReturn(true);

        PahoMqttService service = new PahoMqttService(properties, (uri, clientId) -> mqttClient);
        service.connect();

        ArgumentCaptor<MqttCallbackExtended> callbackCaptor = ArgumentCaptor.forClass(MqttCallbackExtended.class);
        verify(mqttClient).setCallback(callbackCaptor.capture());

        callbackCaptor.getValue().connectionLost(new RuntimeException("network-down"));

        assertThat(service.lifecycleSnapshot().state()).isEqualTo(PahoMqttService.ConnectionState.DISCONNECTED);
        assertThat(service.lifecycleSnapshot().connected()).isFalse();
        assertThat(service.lifecycleSnapshot().lastError()).contains("network-down");
    }

    @Test
    void publish_notConnected_skipsWithWarning(CapturedOutput output) {
        PahoMqttService service = new PahoMqttService(properties, (uri, clientId) -> mqttClient);

        service.publish("/topic", "payload", 1, false);

        assertThat(output).contains("Publish skipped");
    }

    @Test
    void publish_connected_delegatesToClient() throws Exception {
        when(mqttClient.connect(any(MqttConnectOptions.class))).thenReturn(connectToken);
        when(mqttClient.isConnected()).thenReturn(true);
        when(mqttClient.publish(anyString(), any())).thenReturn(deliveryToken);

        PahoMqttService service = new PahoMqttService(properties, (uri, clientId) -> mqttClient);
        service.connect();

        service.publish("/hardware/status", "ok", 1, false);

        verify(mqttClient).publish(eq("/hardware/status"), any());
    }

    @Test
    void publishAsync_connected_delegatesToClientWithoutWaiting() throws Exception {
        when(mqttClient.connect(any(MqttConnectOptions.class))).thenReturn(connectToken);
        when(mqttClient.isConnected()).thenReturn(true);
        when(mqttClient.publish(anyString(), any())).thenReturn(deliveryToken);

        PahoMqttService service = new PahoMqttService(properties, (uri, clientId) -> mqttClient);
        service.connect();

        service.publishAsync("/hardware/status", "ok", 1, false);

        verify(mqttClient).publish(eq("/hardware/status"), any());
        verify(deliveryToken, never()).waitForCompletion(anyLong());
    }

    @Test
    void publish_hardwareMode_logsPayloadToConsole(CapturedOutput output) throws Exception {
        when(mqttClient.connect(any(MqttConnectOptions.class))).thenReturn(connectToken);
        when(mqttClient.isConnected()).thenReturn(true);
        when(mqttClient.publish(anyString(), any())).thenReturn(deliveryToken);

        PahoMqttService service = new PahoMqttService(properties, RunMode.HARDWARE, (uri, clientId) -> mqttClient);
        service.connect();

        service.publish("/hmht/device/N01/command", "{\"command\":\"pump_on\"}", 1, false);

        assertThat(output.getOut())
                .contains("[MQTT][OUT]")
                .contains("/hmht/device/N01/command")
                .contains("{\"command\":\"pump_on\"}");
    }
}
