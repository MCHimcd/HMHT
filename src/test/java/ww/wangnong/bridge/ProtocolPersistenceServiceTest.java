package ww.wangnong.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ww.wangnong.entity.CommandAck;
import ww.wangnong.entity.CommandRequest;
import ww.wangnong.entity.IngestDeadletter;
import ww.wangnong.repository.CommandAckRepository;
import ww.wangnong.repository.CommandRequestRepository;
import ww.wangnong.repository.IngestAuditRepository;
import ww.wangnong.repository.IngestDeadletterRepository;

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
                "{\"payload\":{}}"
        );

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
                "trace-2"
        );

        ArgumentCaptor<CommandAck> ackCaptor = ArgumentCaptor.forClass(CommandAck.class);
        verify(commandAckRepository).save(ackCaptor.capture());
        assertThat(ackCaptor.getValue().getCommandId()).isEqualTo("CMD-0002");
        assertThat(ackCaptor.getValue().getStatus()).isEqualTo("executed");
        assertThat(ackCaptor.getValue().getAckCode()).isEqualTo(0);

        verify(commandRequestRepository).save(any(CommandRequest.class));

        LocalDateTime expectedAckedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ackedAtMs), ZoneId.systemDefault());
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
                "trace-3"
        );

        ArgumentCaptor<IngestDeadletter> captor = ArgumentCaptor.forClass(IngestDeadletter.class);
        verify(ingestDeadletterRepository).save(captor.capture());

        IngestDeadletter saved = captor.getValue();
        assertThat(saved.getSourceTopic()).isEqualTo("/hmht/device/A03_Node_01/telemetry");
        assertThat(saved.getDeviceId()).isEqualTo("A03_Node_01");
        assertThat(saved.getMsgType()).isEqualTo("telemetry");
        assertThat(saved.getReason()).isEqualTo("invalid payload");
        assertThat(saved.getTraceId()).isEqualTo("trace-3");
    }
}
