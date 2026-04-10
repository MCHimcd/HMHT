package ww.wangnong.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ww.wangnong.entity.CommandAck;
import ww.wangnong.entity.CommandRequest;
import ww.wangnong.entity.CapabilityEquipment;
import ww.wangnong.entity.CapabilitySensor;
import ww.wangnong.entity.DeviceShadow;
import ww.wangnong.entity.DeviceProfile;
import ww.wangnong.entity.IngestDeadletter;
import ww.wangnong.entity.Node;
import ww.wangnong.repository.CapabilityEquipmentRepository;
import ww.wangnong.repository.CapabilitySensorRepository;
import ww.wangnong.repository.CommandAckRepository;
import ww.wangnong.repository.CommandRequestRepository;
import ww.wangnong.repository.DeviceCapabilityRepository;
import ww.wangnong.repository.DeviceProfileRepository;
import ww.wangnong.repository.DeviceShadowRepository;
import ww.wangnong.repository.EquipmentTypeRepository;
import ww.wangnong.repository.IngestAuditRepository;
import ww.wangnong.repository.IngestDeadletterRepository;
import ww.wangnong.repository.NodeEquipmentRepository;
import ww.wangnong.repository.NodeRepository;
import ww.wangnong.repository.NodeSensorRepository;
import ww.wangnong.repository.SensorTypeRepository;

@ExtendWith(MockitoExtension.class)
class ProtocolPersistenceServiceTest {

    @Mock
    private CommandRequestRepository commandRequestRepository;

    @Mock
    private CommandAckRepository commandAckRepository;

    @Mock
    private IngestAuditRepository ingestAuditRepository;

    @Mock
    private IngestDeadletterRepository ingestDeadletterRepository;

    @Mock
    private DeviceShadowRepository deviceShadowRepository;

    @Mock
    private DeviceProfileRepository deviceProfileRepository;

    @Mock
    private DeviceCapabilityRepository deviceCapabilityRepository;

    @Mock
    private CapabilitySensorRepository capabilitySensorRepository;

    @Mock
    private CapabilityEquipmentRepository capabilityEquipmentRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private SensorTypeRepository sensorTypeRepository;

    @Mock
    private EquipmentTypeRepository equipmentTypeRepository;

    @Mock
    private NodeSensorRepository nodeSensorRepository;

    @Mock
    private NodeEquipmentRepository nodeEquipmentRepository;

    @InjectMocks
    private ProtocolPersistenceService service;

    @Test
    void recordCommandDispatched_validInput_savesSentRequest() {
        service.recordCommandDispatched(
                "A03_Node_01",
                "CMD-0001",
                "water_pump",
                "turn_on",
                "trace-1",
                5000,
                "/wangnong_2026/node/A03_Node_01/command",
                "/hmht/device/A03_Node_01/command",
                "{\"payload\":{}}");

        ArgumentCaptor<CommandRequest> captor = ArgumentCaptor.forClass(CommandRequest.class);
        verify(commandRequestRepository).save(captor.capture());

        CommandRequest saved = captor.getValue();
        assertThat(saved.getCommandId()).isEqualTo("CMD-0001");
        assertThat(saved.getDeviceId()).isEqualTo("A03_Node_01");
        assertThat(saved.getTargetKey()).isEqualTo("water_pump");
        assertThat(saved.getAction()).isEqualTo("turn_on");
        assertThat(saved.getStatus()).isEqualTo("sent");
        assertThat(saved.getTraceId()).isEqualTo("trace-1");
        assertThat(saved.getTimeoutMs()).isEqualTo(5000);
        assertThat(saved.getSourceTopic()).isEqualTo("/wangnong_2026/node/A03_Node_01/command");
        assertThat(saved.getRelayTopic()).isEqualTo("/hmht/device/A03_Node_01/command");
    }

    @Test
    void recordAckReceived_existingCommand_updatesRequestAndSavesAck() {
        CommandRequest request = new CommandRequest();
        request.setCommandId("CMD-0002");
        request.setStatus("sent");
        when(commandRequestRepository.findByCommandId("CMD-0002")).thenReturn(Optional.of(request));

        long ackedAtMs = 1765000111200L;
        service.recordAckReceived(
                "A03_Node_01",
                "CMD-0002",
                "executed",
                0,
                "ok",
                ackedAtMs,
                "{\"payload\":{}}",
                "/hmht/device/A03_Node_01/ack",
                "trace-2");

        ArgumentCaptor<CommandAck> ackCaptor = ArgumentCaptor.forClass(CommandAck.class);
        verify(commandAckRepository).save(ackCaptor.capture());
        assertThat(ackCaptor.getValue().getCommandId()).isEqualTo("CMD-0002");
        assertThat(ackCaptor.getValue().getStatus()).isEqualTo("executed");
        assertThat(ackCaptor.getValue().getAckCode()).isEqualTo(0);

        verify(commandRequestRepository).save(any(CommandRequest.class));

        ArgumentCaptor<DeviceShadow> shadowCaptor = ArgumentCaptor.forClass(DeviceShadow.class);
        verify(deviceShadowRepository).save(shadowCaptor.capture());
        assertThat(shadowCaptor.getValue().getDeviceId()).isEqualTo("A03_Node_01");
        assertThat(shadowCaptor.getValue().getShadowJson()).contains("CMD-0002");

        LocalDateTime expectedAckedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ackedAtMs),
                ZoneId.systemDefault());
        assertThat(request.getStatus()).isEqualTo("executed");
        assertThat(request.getAckedAt()).isEqualTo(expectedAckedAt);
    }

    @Test
    void recordDeadletter_validInput_savesDeadletter() {
        service.recordDeadletter(
                "/hmht/device/A03_Node_01/telemetry",
                "A03_Node_01",
                "telemetry",
                "invalid payload",
                "{bad-json}",
                "trace-3");

        ArgumentCaptor<IngestDeadletter> captor = ArgumentCaptor.forClass(IngestDeadletter.class);
        verify(ingestDeadletterRepository).save(captor.capture());

        IngestDeadletter saved = captor.getValue();
        assertThat(saved.getSourceTopic()).isEqualTo("/hmht/device/A03_Node_01/telemetry");
        assertThat(saved.getDeviceId()).isEqualTo("A03_Node_01");
        assertThat(saved.getMsgType()).isEqualTo("telemetry");
        assertThat(saved.getReason()).isEqualTo("invalid payload");
        assertThat(saved.getTraceId()).isEqualTo("trace-3");
    }

    @Test
    void recordCapabilitySnapshot_validPayload_savesCapabilityDetails() {
        service.recordCapabilitySnapshot(
                "A03_Node_01",
                "2.0",
                "sha256:abc",
                Map.of("air_temp", Map.of("data_type", "number", "unit", "C", "min", -40, "max", 120)),
                Map.of("water_pump", Map.of("controllable", true, "actions", java.util.List.of("turn_on", "turn_off"))),
                1765000111200L,
                "/hmht/device/A03_Node_01/capability",
                "{\"payload\":{}}",
                "trace-4");

        verify(deviceProfileRepository).save(any());
        verify(deviceCapabilityRepository).save(any());

        ArgumentCaptor<CapabilitySensor> sensorCaptor = ArgumentCaptor.forClass(CapabilitySensor.class);
        verify(capabilitySensorRepository).save(sensorCaptor.capture());
        assertThat(sensorCaptor.getValue().getSensorKey()).isEqualTo("air_temp");

        ArgumentCaptor<CapabilityEquipment> equipmentCaptor = ArgumentCaptor.forClass(CapabilityEquipment.class);
        verify(capabilityEquipmentRepository).save(equipmentCaptor.capture());
        assertThat(equipmentCaptor.getValue().getEquipmentKey()).isEqualTo("water_pump");
    }

    @Test
    void recordTelemetryHeartbeat_updatesDeviceProfileOnlineAndLastSeen() {
        long telemetryAtMs = 1765000111200L;
        when(deviceProfileRepository.findByDeviceId("A03_Node_01")).thenReturn(Optional.empty());

        service.recordTelemetryHeartbeat("A03_Node_01", telemetryAtMs);

        ArgumentCaptor<DeviceProfile> profileCaptor = ArgumentCaptor.forClass(DeviceProfile.class);
        verify(deviceProfileRepository).save(profileCaptor.capture());

        DeviceProfile saved = profileCaptor.getValue();
        assertThat(saved.getDeviceId()).isEqualTo("A03_Node_01");
        assertThat(saved.getOnline()).isTrue();
        assertThat(saved.getLastSeenAt())
                .isEqualTo(LocalDateTime.ofInstant(Instant.ofEpochMilli(telemetryAtMs), ZoneId.systemDefault()));
    }
}
