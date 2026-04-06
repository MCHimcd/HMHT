package ww.wangnong.bridge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

    private static final int BRIDGE_QOS = 1;
    private static final String PY_TELEMETRY = "/agri/device/+/telemetry";
    private static final String PY_AI_ALERT = "/agri/ai_vision/alert";
    private static final String PY_ACK = "/agri/device/+/ack";
    private static final String HW_TELEMETRY = "/hmht/device/+/telemetry";
    private static final String HW_CAPABILITY = "/hmht/device/+/capability";
    private static final String HW_ACK = "/hmht/device/+/ack";
    private static final String APP_COMMAND = "/wangnong_2026/node/+/command";

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
    void init_bridgeMode_registersPythonRoutesOnly() {
        MqttBridge bridge = new MqttBridge(
            mqttService,
            alertService,
            influxTelemetryWriter,
            subscriptionRegistry,
            protocolPersistenceService,
            "bridge");

        bridge.init();

        verify(subscriptionRegistry).register(eq(PY_TELEMETRY), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry).register(eq(PY_AI_ALERT), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry).register(eq(PY_ACK), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry).register(eq(APP_COMMAND), eq(BRIDGE_QOS), any());

        verify(subscriptionRegistry, never()).register(eq(HW_TELEMETRY), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry, never()).register(eq(HW_CAPABILITY), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry, never()).register(eq(HW_ACK), eq(BRIDGE_QOS), any());

        verify(subscriptionRegistry).bindAll(mqttService);
        verifyNoInteractions(alertService, influxTelemetryWriter, protocolPersistenceService);
    }

    @Test
    void init_hardwareMode_registersHardwareRoutesOnly() {
        MqttBridge bridge = new MqttBridge(
            mqttService,
            alertService,
            influxTelemetryWriter,
            subscriptionRegistry,
            protocolPersistenceService,
            "hardware");

        bridge.init();

        verify(subscriptionRegistry).register(eq(HW_TELEMETRY), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry).register(eq(HW_CAPABILITY), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry).register(eq(HW_ACK), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry).register(eq(APP_COMMAND), eq(BRIDGE_QOS), any());

        verify(subscriptionRegistry, never()).register(eq(PY_TELEMETRY), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry, never()).register(eq(PY_AI_ALERT), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry, never()).register(eq(PY_ACK), eq(BRIDGE_QOS), any());

        verify(subscriptionRegistry).bindAll(mqttService);
        verifyNoInteractions(alertService, influxTelemetryWriter, protocolPersistenceService);
    }

    @Test
    void init_fullMode_registersPythonAndHardwareRoutes() {
        MqttBridge bridge = new MqttBridge(
            mqttService,
            alertService,
            influxTelemetryWriter,
            subscriptionRegistry,
            protocolPersistenceService,
            "full");

        bridge.init();

        verify(subscriptionRegistry).register(eq(PY_TELEMETRY), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry).register(eq(PY_AI_ALERT), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry).register(eq(PY_ACK), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry).register(eq(HW_TELEMETRY), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry).register(eq(HW_CAPABILITY), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry).register(eq(HW_ACK), eq(BRIDGE_QOS), any());
        verify(subscriptionRegistry).register(eq(APP_COMMAND), eq(BRIDGE_QOS), any());

        verify(subscriptionRegistry).bindAll(mqttService);
        verifyNoInteractions(alertService, influxTelemetryWriter, protocolPersistenceService);
    }
}
