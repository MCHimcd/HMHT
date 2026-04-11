package ww.wangnong.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import ww.wangnong.config.RunMode;
import ww.wangnong.config.RunModeHolder;
import ww.wangnong.mock.SimNodeSimulator;
import ww.wangnong.mqtt.MqttSubscriptionRegistry;
import ww.wangnong.mqtt.PahoMqttService;
import ww.wangnong.repository.NodeEquipmentRepository;
import ww.wangnong.repository.NodeRepository;
import ww.wangnong.repository.NodeSensorRepository;
import ww.wangnong.service.AlertService;
import ww.wangnong.service.OperationLogService;

@ExtendWith({ MockitoExtension.class, OutputCaptureExtension.class })
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

        @Mock
        private TelemetryAlertChecker alertChecker;

        @Mock
        private SimNodeSimulator simNodeSimulator;

        @Mock
        private NodeRepository nodeRepository;

        @Mock
        private NodeSensorRepository nodeSensorRepository;

        @Mock
        private NodeEquipmentRepository nodeEquipmentRepository;

        @Mock
        private OperationLogService opLogService;

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
                                runModeHolder,
                                alertChecker,
                                simNodeSimulator,
                                nodeRepository,
                                nodeSensorRepository,
                                nodeEquipmentRepository,
                                opLogService);

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
                                runModeHolder,
                                alertChecker,
                                simNodeSimulator,
                                nodeRepository,
                                nodeSensorRepository,
                                nodeEquipmentRepository,
                                opLogService);

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
                                runModeHolder,
                                alertChecker,
                                simNodeSimulator,
                                nodeRepository,
                                nodeSensorRepository,
                                nodeEquipmentRepository,
                                opLogService);

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

        @Test
        void hardwareInboundMessages_allKinds_logsRawMqttEnvelope(CapturedOutput output) {
                when(runModeHolder.isPythonRouteEnabled()).thenReturn(false);
                when(runModeHolder.isHardwareRouteEnabled()).thenReturn(true);
                when(runModeHolder.getMode()).thenReturn(RunMode.HARDWARE);

                MqttBridge bridge = new MqttBridge(
                                mqttService,
                                alertService,
                                influxTelemetryWriter,
                                subscriptionRegistry,
                                protocolPersistenceService,
                                runModeHolder,
                                alertChecker,
                                simNodeSimulator,
                                nodeRepository,
                                nodeSensorRepository,
                                nodeEquipmentRepository,
                                opLogService);

                bridge.init();

                Consumer<PahoMqttService.InboundMessage> telemetryHandler = captureHandler(HW_TELEMETRY);
                Consumer<PahoMqttService.InboundMessage> capabilityHandler = captureHandler(HW_CAPABILITY);
                Consumer<PahoMqttService.InboundMessage> ackHandler = captureHandler(HW_ACK);

                telemetryHandler.accept(new PahoMqttService.InboundMessage(
                                "/hmht/device/N01/telemetry",
                                "{\"device_id\":\"N01\",\"protocol_version\":\"2.0\",\"trace_id\":\"trace-tel\",\"timestamp\":1765000111200,\"payload\":{\"sensors\":{\"air_temp\":{\"value\":25.5}},\"equipments\":{\"water_pump\":{\"state\":true}},\"mode\":\"auto\"}}",
                                1,
                                false));

                capabilityHandler.accept(new PahoMqttService.InboundMessage(
                                "/hmht/device/N01/capability",
                                "{\"device_id\":\"N01\",\"protocol_version\":\"2.0\",\"trace_id\":\"trace-cap\",\"timestamp\":1765000111200,\"payload\":{\"capability_version\":\"2.0\",\"sensors\":{},\"equipments\":{}}}",
                                1,
                                false));

                ackHandler.accept(new PahoMqttService.InboundMessage(
                                "/hmht/device/N01/ack",
                                "{\"device_id\":\"N01\",\"protocol_version\":\"2.0\",\"trace_id\":\"trace-ack\",\"payload\":{\"command_id\":\"CMD-1\",\"status\":\"executed\",\"ack_code\":0,\"ack_message\":\"ok\",\"acked_at\":1765000111200}}",
                                1,
                                false));

                assertThat(output.getAll()).contains("[Bridge][HW-IN]");
                assertThat(output.getAll()).contains("/hmht/device/N01/telemetry");
                assertThat(output.getAll()).contains("/hmht/device/N01/capability");
                assertThat(output.getAll()).contains("/hmht/device/N01/ack");
                assertThat(output.getAll()).contains("qos=1");
                assertThat(output.getAll()).contains("retained=false");
        }

        @Test
        void appCommand_hardwareMode_forwardsToHardwareTopicViaPublishAsync() {
                when(runModeHolder.isPythonRouteEnabled()).thenReturn(false);
                when(runModeHolder.isHardwareRouteEnabled()).thenReturn(true);
                when(runModeHolder.getMode()).thenReturn(RunMode.HARDWARE);

                MqttBridge bridge = new MqttBridge(
                                mqttService,
                                alertService,
                                influxTelemetryWriter,
                                subscriptionRegistry,
                                protocolPersistenceService,
                                runModeHolder,
                                alertChecker,
                                simNodeSimulator,
                                nodeRepository,
                                nodeSensorRepository,
                                nodeEquipmentRepository,
                                opLogService);

                bridge.init();

                Consumer<PahoMqttService.InboundMessage> commandHandler = captureHandler(APP_COMMAND);
                commandHandler.accept(new PahoMqttService.InboundMessage(
                                "/wangnong_2026/node/A03_Node_01/command",
                                "{\"cmd_id\":\"app_1\",\"timestamp\":1775837206642,\"msg_type\":\"control\",\"action\":{\"target_device\":\"mode\",\"command\":\"smart\",\"parameters\":{\"duration_seconds\":15}}}",
                                1,
                                false));

                verify(mqttService).publishAsync(eq("/hmht/device/A03_Node_01/command"), any(), eq(1), eq(false));
                verify(mqttService, never()).publish(eq("/hmht/device/A03_Node_01/command"), any(), eq(1), eq(false));
                verify(protocolPersistenceService, times(1)).recordCommandDispatched(
                                eq("A03_Node_01"),
                                any(),
                                eq("mode"),
                                eq("smart"),
                                any(),
                                eq(5000),
                                eq("/wangnong_2026/node/A03_Node_01/command"),
                                eq("/hmht/device/A03_Node_01/command"),
                                any());
        }

        @Test
        void hardwareTelemetry_hardwareMode_forwardsToAppTopicViaPublishAsync() {
                when(runModeHolder.isPythonRouteEnabled()).thenReturn(false);
                when(runModeHolder.isHardwareRouteEnabled()).thenReturn(true);
                when(runModeHolder.getMode()).thenReturn(RunMode.HARDWARE);

                MqttBridge bridge = new MqttBridge(
                                mqttService,
                                alertService,
                                influxTelemetryWriter,
                                subscriptionRegistry,
                                protocolPersistenceService,
                                runModeHolder,
                                alertChecker,
                                simNodeSimulator,
                                nodeRepository,
                                nodeSensorRepository,
                                nodeEquipmentRepository,
                                opLogService);

                bridge.init();

                Consumer<PahoMqttService.InboundMessage> hwTelemetryHandler = captureHandler(HW_TELEMETRY);
                hwTelemetryHandler.accept(new PahoMqttService.InboundMessage(
                                "/hmht/device/A03_Node_01/telemetry",
                                "{\"msg_type\":\"telemetry\",\"protocol_version\":\"2.0\",\"device_id\":\"A03_Node_01\",\"timestamp\":1484747,\"trace_id\":\"hw-trace\",\"payload\":{\"sensors\":{\"air_temp\":{\"value\":24.2,\"unit\":\"C\"}},\"equipments\":{\"water_pump\":{\"state\":\"on\"}},\"mode\":\"smart\"}}",
                                1,
                                false));

                verify(mqttService).publishAsync(eq("/wangnong_2026/node/A03_Node_01/telemetry"), any(), eq(1),
                                eq(false));
                verify(mqttService, never()).publish(eq("/wangnong_2026/node/A03_Node_01/telemetry"), any(), eq(1),
                                eq(false));

                InOrder inOrder = inOrder(mqttService, protocolPersistenceService);
                inOrder.verify(mqttService).publishAsync(eq("/wangnong_2026/node/A03_Node_01/telemetry"), any(),
                                eq(1), eq(false));
                inOrder.verify(protocolPersistenceService).recordTelemetryHeartbeat(eq("A03_Node_01"), eq(1484747L));
        }

        @Test
        void fullMode_pythonTelemetry_isIgnoredAfterRealHardwareTelemetryForSameDevice() {
                when(runModeHolder.isPythonRouteEnabled()).thenReturn(true);
                when(runModeHolder.isHardwareRouteEnabled()).thenReturn(true);
                when(runModeHolder.getMode()).thenReturn(RunMode.FULL);

                MqttBridge bridge = new MqttBridge(
                                mqttService,
                                alertService,
                                influxTelemetryWriter,
                                subscriptionRegistry,
                                protocolPersistenceService,
                                runModeHolder,
                                alertChecker,
                                simNodeSimulator,
                                nodeRepository,
                                nodeSensorRepository,
                                nodeEquipmentRepository,
                                opLogService);

                bridge.init();

                Consumer<PahoMqttService.InboundMessage> hwTelemetryHandler = captureHandler(HW_TELEMETRY);
                Consumer<PahoMqttService.InboundMessage> pyTelemetryHandler = captureHandler(PY_TELEMETRY);

                hwTelemetryHandler.accept(new PahoMqttService.InboundMessage(
                                "/hmht/device/A03_Node_01/telemetry",
                                "{\"msg_type\":\"telemetry\",\"protocol_version\":\"2.0\",\"device_id\":\"A03_Node_01\",\"timestamp\":1484747,\"trace_id\":\"hw-trace\",\"payload\":{\"sensors\":{\"air_temp\":{\"value\":24.2,\"unit\":\"C\"}},\"equipments\":{\"water_pump\":{\"state\":\"on\"}},\"mode\":\"smart\"}}",
                                1,
                                false));

                pyTelemetryHandler.accept(new PahoMqttService.InboundMessage(
                                "/agri/device/A03_Node_01/telemetry",
                                "{\"device_id\":\"A03_Node_01\",\"timestamp\":932406,\"msg_type\":\"telemetry\",\"data\":{\"environment\":{\"air_temp\":23.8},\"equipment_status\":{\"water_pump\":false,\"fan\":false},\"system_mode\":\"manual\"}}",
                                1,
                                false));

                verify(mqttService, times(1)).publishAsync(eq("/wangnong_2026/node/A03_Node_01/telemetry"), any(),
                                eq(1), eq(false));
                verify(mqttService, never()).publish(eq("/wangnong_2026/node/A03_Node_01/telemetry"), any(), eq(1),
                                eq(false));
                verify(protocolPersistenceService).recordIngestAudit(
                                eq("telemetry"),
                                eq("/agri/device/A03_Node_01/telemetry"),
                                eq("A03_Node_01"),
                                eq(""),
                                eq(""),
                                eq("ignored_hardware_preferred"),
                                eq(null));
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private Consumer<PahoMqttService.InboundMessage> captureHandler(String topicFilter) {
                ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
                verify(subscriptionRegistry).register(eq(topicFilter), eq(BRIDGE_QOS), captor.capture());
                return (Consumer<PahoMqttService.InboundMessage>) captor.getValue();
        }
}
