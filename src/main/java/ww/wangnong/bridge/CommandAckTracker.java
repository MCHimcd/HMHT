package ww.wangnong.bridge;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ww.wangnong.config.condition.ConditionalOnHardwareRoute;
import ww.wangnong.entity.CommandRequest;
import ww.wangnong.entity.CommandRetryLog;
import ww.wangnong.entity.DeviceProfile;
import ww.wangnong.mqtt.PahoMqttService;
import ww.wangnong.repository.CommandRequestRepository;
import ww.wangnong.repository.CommandRetryLogRepository;
import ww.wangnong.repository.DeviceProfileRepository;

/**
 * 命令 ACK 超时重试追踪器。
 * 在硬件路由启用时激活（hardware/full 模式）。
 */
@Component
@ConditionalOnHardwareRoute
public class CommandAckTracker {

    private static final Logger log = LoggerFactory.getLogger(CommandAckTracker.class);
    private static final int BRIDGE_QOS = 1;

    private final CommandRequestRepository commandRequestRepository;
    private final CommandRetryLogRepository commandRetryLogRepository;
    private final DeviceProfileRepository deviceProfileRepository;
    private final PahoMqttService mqttService;
    private final int maxRetries;
    private final int defaultTimeoutMs;
    private final int batchSize;
    private final int deviceOfflineThresholdMs;

    public CommandAckTracker(
            CommandRequestRepository commandRequestRepository,
            CommandRetryLogRepository commandRetryLogRepository,
            DeviceProfileRepository deviceProfileRepository,
            PahoMqttService mqttService,
            @Value("${hmht.command.retry.max-retries:3}") int maxRetries,
            @Value("${hmht.command.retry.default-timeout-ms:5000}") int defaultTimeoutMs,
            @Value("${hmht.command.retry.batch-size:200}") int batchSize,
            @Value("${hmht.command.retry.device-offline-threshold-ms:30000}") int deviceOfflineThresholdMs) {
        this.commandRequestRepository = commandRequestRepository;
        this.commandRetryLogRepository = commandRetryLogRepository;
        this.deviceProfileRepository = deviceProfileRepository;
        this.mqttService = mqttService;
        this.maxRetries = maxRetries;
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.batchSize = batchSize;
        this.deviceOfflineThresholdMs = deviceOfflineThresholdMs;
    }

    @Scheduled(fixedDelayString = "${hmht.command.retry.check-interval-ms:5000}", initialDelayString = "${hmht.command.retry.initial-delay-ms:3000}")
    public void checkTimeoutAndRetry() {
        List<CommandRequest> pending = commandRequestRepository
                .findTop200ByStatusInOrderBySentAtAsc(List.of("sent", "retry"));
        if (pending.isEmpty()) {
            return;
        }

        int limit = Math.min(batchSize, pending.size());
        LocalDateTime now = LocalDateTime.now();
        boolean mqttOverloaded = false;
        for (int i = 0; i < limit; i++) {
            CommandRequest request = pending.get(i);
            try {
                if (!isTimedOut(request, now)) {
                    continue;
                }
                if (mqttOverloaded) {
                    // MQTT in-flight queue is full; skip remaining retries until next cycle
                    break;
                }
                processTimedOutRequest(request, now);
            } catch (MqttOverloadedException e) {
                log.warn("[AckTracker] MQTT overloaded, deferring remaining retries to next cycle. commandId={}",
                        request.getCommandId());
                mqttOverloaded = true;
            } catch (Exception e) {
                log.warn("[AckTracker] Retry handling failed: commandId={}", request.getCommandId(), e);
            }
        }
    }

    private boolean isTimedOut(CommandRequest request, LocalDateTime now) {
        if (request.getSentAt() == null) {
            return false;
        }
        int timeoutMs = request.getTimeoutMs() == null || request.getTimeoutMs() <= 0
                ? defaultTimeoutMs
                : request.getTimeoutMs();
        LocalDateTime expiredAt = request.getSentAt().plus(timeoutMs, ChronoUnit.MILLIS);
        return !expiredAt.isAfter(now);
    }

    private void processTimedOutRequest(CommandRequest request, LocalDateTime now) {
        int nextAttempt = safeRetryCount(request) + 1;
        if (safeRetryCount(request) < maxRetries) {
            if (!mqttService.isConnected()) {
                log.debug("[AckTracker] MQTT not connected, skipping retry: commandId={}", request.getCommandId());
                return;
            }

            if (isDeviceOffline(request.getDeviceId(), now)) {
                recordRetryLog(request.getCommandId(), nextAttempt, "device_offline", now);
                request.setRetryCount(nextAttempt);
                request.setStatus("timeout");
                commandRequestRepository.save(request);
                log.warn("[AckTracker] Device appears offline, command finalized: commandId={}, deviceId={}",
                        request.getCommandId(), request.getDeviceId());
                return;
            }

            String topic = "/hmht/device/" + request.getDeviceId() + "/command";
            String rawPayload = request.getRawPayload() == null ? "{}" : request.getRawPayload();
            try {
                mqttService.publish(topic, rawPayload, BRIDGE_QOS, false);
            } catch (Exception e) {
                if (isMqttOverloaded(e)) {
                    throw new MqttOverloadedException(e);
                }
                log.warn("[AckTracker] Publish failed during retry, will re-attempt next cycle: commandId={}",
                        request.getCommandId(), e);
                return;
            }

            // Only update DB state after successful publish
            recordRetryLog(request.getCommandId(), nextAttempt, "timeout_exceeded", now);
            request.setRetryCount(nextAttempt);
            request.setStatus("retry");
            request.setSentAt(now);
            commandRequestRepository.save(request);

            log.info("[AckTracker] Command retried: commandId={}, attempt={}", request.getCommandId(), nextAttempt);
            return;
        }

        recordRetryLog(request.getCommandId(), nextAttempt, "max_retries_exceeded", now);
        request.setRetryCount(nextAttempt);
        request.setStatus("timeout");
        commandRequestRepository.save(request);

        log.warn("[AckTracker] Command timeout finalized: commandId={}, retries={}",
                request.getCommandId(), safeRetryCount(request));
    }

    private void recordRetryLog(String commandId, int attemptNo, String reason, LocalDateTime retryAt) {
        CommandRetryLog logEntity = new CommandRetryLog();
        logEntity.setCommandId(commandId);
        logEntity.setAttemptNo(attemptNo);
        logEntity.setRetryReason(reason);
        logEntity.setRetryAt(retryAt);
        commandRetryLogRepository.save(logEntity);
    }

    private int safeRetryCount(CommandRequest request) {
        return request.getRetryCount() == null ? 0 : request.getRetryCount();
    }

    private boolean isDeviceOffline(String deviceId, LocalDateTime now) {
        if (deviceId == null || deviceId.isBlank()) {
            return false;
        }
        return deviceProfileRepository.findByDeviceId(deviceId)
                .map(profile -> {
                    if (profile.getLastSeenAt() == null) {
                        return true;
                    }
                    LocalDateTime threshold = now.minus(deviceOfflineThresholdMs, ChronoUnit.MILLIS);
                    return profile.getLastSeenAt().isBefore(threshold);
                })
                .orElse(false);
    }

    private boolean isMqttOverloaded(Exception e) {
        // Paho error code 32202 = "Too many publishes in progress"
        Throwable cause = e.getCause();
        if (cause instanceof org.eclipse.paho.client.mqttv3.MqttException mqttEx) {
            return mqttEx.getReasonCode() == 32202;
        }
        return false;
    }

    static class MqttOverloadedException extends RuntimeException {
        MqttOverloadedException(Throwable cause) {
            super(cause);
        }
    }
}
