package ww.wangnong.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ww.wangnong.TestDataFactory;
import ww.wangnong.entity.NodeSensor;
import ww.wangnong.entity.SensorType;
import ww.wangnong.mqtt.PahoMqttService;
import ww.wangnong.repository.NodeSensorRepository;
import ww.wangnong.repository.SensorTypeRepository;
import ww.wangnong.service.AlertService;

@ExtendWith(MockitoExtension.class)
class TelemetryAlertCheckerTest {

    @Mock
    private NodeSensorRepository nodeSensorRepo;
    @Mock
    private SensorTypeRepository sensorTypeRepo;
    @Mock
    private AlertService alertService;
    @Mock
    private PahoMqttService mqttService;

    private TelemetryAlertChecker checker;

    @BeforeEach
    void setUp() {
        checker = new TelemetryAlertChecker(nodeSensorRepo, sensorTypeRepo, alertService, mqttService);
    }

    @Test
    void checkAndAlertEnv_valueAboveMax_triggersAlertAndPublishes() {
        NodeSensor ns = TestDataFactory.nodeSensor("N01", "temperature", 10.0, 35.0);
        when(nodeSensorRepo.findByNodeId("N01")).thenReturn(List.of(ns));
        when(sensorTypeRepo.findBySensorKey("temperature"))
                .thenReturn(Optional.of(TestDataFactory.sensorType("temperature", "空气温度", "℃")));

        checker.checkAndAlertEnv("N01", Map.of("temperature", 40.0));

        ArgumentCaptor<AlertService.AlertInfo> captor = ArgumentCaptor.forClass(AlertService.AlertInfo.class);
        verify(alertService).addAlert(captor.capture());
        AlertService.AlertInfo info = captor.getValue();
        assertThat(info.nodeId()).isEqualTo("N01");
        assertThat(info.level()).isEqualTo("warning");
        assertThat(info.message()).contains("超过阈值");

        verify(mqttService).publish(eq("/wangnong_2026/node/N01/alert"), anyString(), eq(1), eq(false));
    }

    @Test
    void checkAndAlertEnv_valueBelowMin_triggersAlert() {
        NodeSensor ns = TestDataFactory.nodeSensor("N01", "temperature", 10.0, 35.0);
        when(nodeSensorRepo.findByNodeId("N01")).thenReturn(List.of(ns));
        when(sensorTypeRepo.findBySensorKey("temperature"))
                .thenReturn(Optional.of(TestDataFactory.sensorType("temperature", "空气温度", "℃")));

        checker.checkAndAlertEnv("N01", Map.of("temperature", 5.0));

        ArgumentCaptor<AlertService.AlertInfo> captor = ArgumentCaptor.forClass(AlertService.AlertInfo.class);
        verify(alertService).addAlert(captor.capture());
        assertThat(captor.getValue().message()).contains("低于阈值");
    }

    @Test
    void checkAndAlertEnv_valueWithinRange_noAlert() {
        NodeSensor ns = TestDataFactory.nodeSensor("N01", "temperature", 10.0, 35.0);
        when(nodeSensorRepo.findByNodeId("N01")).thenReturn(List.of(ns));

        checker.checkAndAlertEnv("N01", Map.of("temperature", 25.0));

        verify(alertService, never()).addAlert(any());
        verify(mqttService, never()).publish(anyString(), anyString(), anyInt(), any(Boolean.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkAndAlertEnv_withinCooldown_skipsRepeatAlert() throws Exception {
        NodeSensor ns = TestDataFactory.nodeSensor("N01", "temperature", 10.0, 35.0);
        when(nodeSensorRepo.findByNodeId("N01")).thenReturn(List.of(ns));
        when(sensorTypeRepo.findBySensorKey("temperature"))
                .thenReturn(Optional.of(TestDataFactory.sensorType("temperature", "空气温度", "℃")));

        // Inject a recent cooldown timestamp via reflection
        Field lastAlertTimeField = TelemetryAlertChecker.class.getDeclaredField("lastAlertTime");
        lastAlertTimeField.setAccessible(true);
        ConcurrentHashMap<String, Long> lastAlertTime = (ConcurrentHashMap<String, Long>) lastAlertTimeField
                .get(checker);
        lastAlertTime.put("N01:temperature", System.currentTimeMillis());

        checker.checkAndAlertEnv("N01", Map.of("temperature", 40.0));

        verify(alertService, never()).addAlert(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkAndAlertEnv_afterCooldown_triggersAgain() throws Exception {
        NodeSensor ns = TestDataFactory.nodeSensor("N01", "temperature", 10.0, 35.0);
        when(nodeSensorRepo.findByNodeId("N01")).thenReturn(List.of(ns));
        when(sensorTypeRepo.findBySensorKey("temperature"))
                .thenReturn(Optional.of(TestDataFactory.sensorType("temperature", "空气温度", "℃")));

        // Inject an expired cooldown timestamp (6 minutes ago)
        Field lastAlertTimeField = TelemetryAlertChecker.class.getDeclaredField("lastAlertTime");
        lastAlertTimeField.setAccessible(true);
        ConcurrentHashMap<String, Long> lastAlertTime = (ConcurrentHashMap<String, Long>) lastAlertTimeField
                .get(checker);
        lastAlertTime.put("N01:temperature", System.currentTimeMillis() - 6 * 60_000L);

        checker.checkAndAlertEnv("N01", Map.of("temperature", 40.0));

        verify(alertService).addAlert(any());
    }

    @Test
    void checkAndAlertEnv_sensorTypeNotFound_usesKeyAsName() {
        NodeSensor ns = TestDataFactory.nodeSensor("N01", "temperature", 10.0, 35.0);
        when(nodeSensorRepo.findByNodeId("N01")).thenReturn(List.of(ns));
        when(sensorTypeRepo.findBySensorKey("temperature")).thenReturn(Optional.empty());

        checker.checkAndAlertEnv("N01", Map.of("temperature", 40.0));

        ArgumentCaptor<AlertService.AlertInfo> captor = ArgumentCaptor.forClass(AlertService.AlertInfo.class);
        verify(alertService).addAlert(captor.capture());
        // When SensorType is missing, sensorKey itself is used as name
        assertThat(captor.getValue().message()).contains("temperature");
    }

    @Test
    void checkAndAlertEnv_nullWarningMax_skipsMaxCheck() {
        NodeSensor ns = TestDataFactory.nodeSensor("N01", "temperature", 10.0, null);
        when(nodeSensorRepo.findByNodeId("N01")).thenReturn(List.of(ns));

        checker.checkAndAlertEnv("N01", Map.of("temperature", 99.0));

        verify(alertService, never()).addAlert(any());
    }

    @Test
    void checkAndAlert_invalidDataStructure_returnsEarly() {
        // No "data" key
        checker.checkAndAlert("N01", Map.of("msg_type", "telemetry"));
        // "data" is not a Map
        checker.checkAndAlert("N01", Map.of("data", "string-value"));
        // "data" has no "environment"
        checker.checkAndAlert("N01", Map.of("data", Map.of("equipment", Map.of())));

        verify(nodeSensorRepo, never()).findByNodeId(anyString());
    }
}
