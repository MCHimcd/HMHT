package ww.wangnong.bridge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ww.wangnong.mqtt.MqttSubscriptionRegistry;
import ww.wangnong.mqtt.PahoMqttService;
import ww.wangnong.service.AlertService;

@ExtendWith(MockitoExtension.class)
class MqttBridgeTest {

    @Mock
    private PahoMqttService mqttService;

    @Mock
    private AlertService alertService;

    @Mock
    private InfluxTelemetryWriter influxTelemetryWriter;

    @Mock
    private MqttSubscriptionRegistry subscriptionRegistry;

    @Mock
    private ProtocolPersistenceService protocolPersistenceService;

    @Test
    void init_registersAllBridgeRoutesThroughRegistry() {
        MqttBridge bridge = new MqttBridge(
            mqttService,
            alertService,
            influxTelemetryWriter,
            subscriptionRegistry,
            protocolPersistenceService);

        bridge.init();

        verify(subscriptionRegistry).register(eq("/agri/device/+/telemetry"), eq(1), any());
        verify(subscriptionRegistry).register(eq("/hmht/device/+/telemetry"), eq(1), any());
        verify(subscriptionRegistry).register(eq("/agri/ai_vision/alert"), eq(1), any());
        verify(subscriptionRegistry).register(eq("/wangnong_2026/node/+/command"), eq(1), any());
        verify(subscriptionRegistry).register(eq("/agri/device/+/ack"), eq(1), any());
        verify(subscriptionRegistry).register(eq("/hmht/device/+/ack"), eq(1), any());
        verify(subscriptionRegistry).bindAll(mqttService);
        verifyNoInteractions(alertService, influxTelemetryWriter, protocolPersistenceService);
    }
}
