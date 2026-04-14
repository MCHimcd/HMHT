package ww.wangnong.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ww.wangnong.bridge.InfluxTelemetryWriter;
import ww.wangnong.bridge.TelemetryAlertChecker;
import ww.wangnong.config.RunModeHolder;
import ww.wangnong.mqtt.PahoMqttService;
import ww.wangnong.repository.NodeRepository;

@ExtendWith(MockitoExtension.class)
class SimNodeSimulatorTest {

    @Mock
    private PahoMqttService mqttService;
    @Mock
    private TelemetryAlertChecker alertChecker;
    @Mock
    private InfluxTelemetryWriter influxWriter;
    @Mock
    private NodeRepository nodeRepo;

    private SimNodeSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new SimNodeSimulator(
                mqttService,
                alertChecker,
                influxWriter,
                nodeRepo,
                new RunModeHolder("mock"),
                true);
    }

    @Test
    void init_frontendDriven_doesNotSubscribeCommandTopic() {
        simulator.init();

        verify(mqttService, never()).subscribe(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishTelemetry_frontendDriven_doesNotPublishTelemetry() {
        simulator.publishTelemetry();

        verify(mqttService, never()).publish(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void checkThresholds_frontendDriven_doesNotCreateBackendAlerts() {
        simulator.checkThresholds();

        verify(alertChecker, never()).checkAndAlertEnv(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyMap());
    }
}