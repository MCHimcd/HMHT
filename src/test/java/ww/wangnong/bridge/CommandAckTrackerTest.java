package ww.wangnong.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ww.wangnong.entity.CommandRequest;
import ww.wangnong.entity.CommandRetryLog;
import ww.wangnong.repository.CommandRequestRepository;
import ww.wangnong.repository.CommandRetryLogRepository;
import ww.wangnong.repository.DeviceProfileRepository;
import ww.wangnong.mqtt.PahoMqttService;

@ExtendWith(MockitoExtension.class)
class CommandAckTrackerTest {

        @Mock
        private CommandRequestRepository commandRequestRepository;

        @Mock
        private CommandRetryLogRepository commandRetryLogRepository;

        @Mock
        private DeviceProfileRepository deviceProfileRepository;

        @Mock
        private PahoMqttService mqttService;

        @Test
        void checkTimeoutAndRetry_timedOutRequest_retriesAndRecordsLog() {
                CommandRequest request = new CommandRequest();
                request.setCommandId("CMD-001");
                request.setDeviceId("D01");
                request.setStatus("sent");
                request.setTimeoutMs(5000);
                request.setRetryCount(0);
                request.setSentAt(LocalDateTime.now().minusSeconds(10));
                request.setRawPayload("{\"payload\":{}}\n");

                when(commandRequestRepository.findTop200ByStatusInOrderBySentAtAsc(List.of("sent", "retry")))
                                .thenReturn(List.of(request));
                when(mqttService.isConnected()).thenReturn(true);

                CommandAckTracker tracker = new CommandAckTracker(
                                commandRequestRepository,
                                commandRetryLogRepository,
                                deviceProfileRepository,
                                mqttService,
                                3,
                                5000,
                                200,
                                30000);

                tracker.checkTimeoutAndRetry();

                verify(mqttService).publish(eq("/hmht/device/D01/command"), eq("{\"payload\":{}}\n"), eq(1), eq(false));

                ArgumentCaptor<CommandRequest> reqCaptor = ArgumentCaptor.forClass(CommandRequest.class);
                verify(commandRequestRepository).save(reqCaptor.capture());
                assertThat(reqCaptor.getValue().getRetryCount()).isEqualTo(1);
                assertThat(reqCaptor.getValue().getStatus()).isEqualTo("retry");

                ArgumentCaptor<CommandRetryLog> logCaptor = ArgumentCaptor.forClass(CommandRetryLog.class);
                verify(commandRetryLogRepository).save(logCaptor.capture());
                assertThat(logCaptor.getValue().getRetryReason()).isEqualTo("timeout_exceeded");
                assertThat(logCaptor.getValue().getAttemptNo()).isEqualTo(1);
        }

        @Test
        void checkTimeoutAndRetry_reachMaxRetries_marksTimeoutWithoutRepublish() {
                CommandRequest request = new CommandRequest();
                request.setCommandId("CMD-002");
                request.setDeviceId("D02");
                request.setStatus("retry");
                request.setTimeoutMs(5000);
                request.setRetryCount(3);
                request.setSentAt(LocalDateTime.now().minusSeconds(10));
                request.setRawPayload("{\"payload\":{}}\n");

                when(commandRequestRepository.findTop200ByStatusInOrderBySentAtAsc(List.of("sent", "retry")))
                                .thenReturn(List.of(request));

                CommandAckTracker tracker = new CommandAckTracker(
                                commandRequestRepository,
                                commandRetryLogRepository,
                                deviceProfileRepository,
                                mqttService,
                                3,
                                5000,
                                200,
                                30000);

                tracker.checkTimeoutAndRetry();

                verify(mqttService, never()).publish(any(), any(), anyInt(), anyBoolean());

                ArgumentCaptor<CommandRequest> reqCaptor = ArgumentCaptor.forClass(CommandRequest.class);
                verify(commandRequestRepository).save(reqCaptor.capture());
                assertThat(reqCaptor.getValue().getStatus()).isEqualTo("timeout");

                ArgumentCaptor<CommandRetryLog> logCaptor = ArgumentCaptor.forClass(CommandRetryLog.class);
                verify(commandRetryLogRepository).save(logCaptor.capture());
                assertThat(logCaptor.getValue().getRetryReason()).isEqualTo("max_retries_exceeded");
                assertThat(logCaptor.getValue().getAttemptNo()).isEqualTo(4);
        }

        @Test
        void checkTimeoutAndRetry_notTimedOut_skipsProcessing() {
                CommandRequest request = new CommandRequest();
                request.setCommandId("CMD-003");
                request.setDeviceId("D03");
                request.setStatus("sent");
                request.setTimeoutMs(5000);
                request.setRetryCount(0);
                request.setSentAt(LocalDateTime.now().minusSeconds(2));
                request.setRawPayload("{\"payload\":{}}\n");

                when(commandRequestRepository.findTop200ByStatusInOrderBySentAtAsc(List.of("sent", "retry")))
                                .thenReturn(List.of(request));

                CommandAckTracker tracker = new CommandAckTracker(
                                commandRequestRepository,
                                commandRetryLogRepository,
                                deviceProfileRepository,
                                mqttService,
                                3,
                                5000,
                                200,
                                30000);

                tracker.checkTimeoutAndRetry();

                verify(commandRetryLogRepository, never()).save(any());
                verify(commandRequestRepository, never()).save(any());
                verify(mqttService, never()).publish(any(), any(), anyInt(), anyBoolean());
        }
}
