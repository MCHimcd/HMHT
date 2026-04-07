package ww.wangnong.bridge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ww.wangnong.config.RunMode;
import ww.wangnong.config.RunModeHolder;
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

    @Mock
    private RunModeHolder runModeHolder;

    @Test
    void init_bridgeMode_registersPythonRoutesOnly() {
        when(runModeHolder.isPythonRouteEnabled()).thenReturn(true);
        when(runModeHolder.isHardwareRouteEnabled()).thenReturn(false);
        when(runModeHolder.getMode()).thenReturn(RunMode.BRIDGE);

        MqttBridge bridge = new MqttBridge(
            mqttService,
            alertService,
            influxTelemetryWriter,
            subscriptionRegistry,
            protocolPersistenceService,
            runModeHolder);

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
        when(runModeHolder.isPythonRouteEnabled()).thenReturn(false);
        when(runModeHolder.isHardwareRouteEnabled()).thenReturn(true);
        when(runModeHolder.getMode()).thenReturn(RunMode.HARDWARE);

        MqttBridge bridge = new MqttBridge(
            mqttService,
            alertService,
            influxTelemetryWriter,
            subscriptionRegistry,
            protocolPersistenceService,
            runModeHolder);

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
        when(runModeHolder.isPythonRouteEnabled()).thenReturn(true);
        when(runModeHolder.isHardwareRouteEnabled()).thenReturn(true);
        when(runModeHolder.getMode()).thenReturn(RunMode.FULL);

        MqttBridge bridge = new MqttBridge(
            mqttService,
            alertService,
            influxTelemetryWriter,
            subscriptionRegistry,
            protocolPersistenceService,
            runModeHolder);

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
