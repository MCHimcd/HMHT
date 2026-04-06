package ww.wangnong.bridge;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ww.wangnong.entity.CommandAck;
import ww.wangnong.entity.CommandRequest;
import ww.wangnong.entity.IngestAudit;
import ww.wangnong.entity.IngestDeadletter;
import ww.wangnong.repository.CommandAckRepository;
import ww.wangnong.repository.CommandRequestRepository;
import ww.wangnong.repository.IngestAuditRepository;
import ww.wangnong.repository.IngestDeadletterRepository;

@Service
@ConditionalOnProperty(name = "hmht.run.mode", havingValue = "bridge")
public class ProtocolPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ProtocolPersistenceService.class);

    private final CommandRequestRepository commandRequestRepository;
    private final CommandAckRepository commandAckRepository;
    private final IngestAuditRepository ingestAuditRepository;
    private final IngestDeadletterRepository ingestDeadletterRepository;

    public ProtocolPersistenceService(
            CommandRequestRepository commandRequestRepository,
            CommandAckRepository commandAckRepository,
            IngestAuditRepository ingestAuditRepository,
            IngestDeadletterRepository ingestDeadletterRepository) {
        this.commandRequestRepository = commandRequestRepository;
        this.commandAckRepository = commandAckRepository;
        this.ingestAuditRepository = ingestAuditRepository;
        this.ingestDeadletterRepository = ingestDeadletterRepository;
    }

    public void recordCommandDispatched(
            String deviceId,
            String commandId,
            String targetKey,
            String action,
            String traceId,
            Integer timeoutMs,
            String sourceTopic,
            String relayTopic,
            String rawPayload) {
        try {
            CommandRequest request = commandRequestRepository.findByCommandId(commandId).orElseGet(CommandRequest::new);
            request.setCommandId(commandId);
            request.setDeviceId(deviceId);
            request.setTargetKey(defaultIfBlank(targetKey, "unknown"));
            request.setAction(defaultIfBlank(action, "noop"));
            request.setStatus("sent");
            request.setTraceId(traceId);
            request.setTimeoutMs(timeoutMs);
            request.setSourceTopic(sourceTopic);
            request.setRelayTopic(relayTopic);
            request.setRawPayload(rawPayload);
            request.setSentAt(LocalDateTime.now());
            commandRequestRepository.save(request);
        } catch (Exception e) {
            log.warn("[ProtocolPersistence] Failed to persist command request: commandId={}", commandId, e);
        }
    }

    public void recordAckReceived(
            String deviceId,
            String commandId,
            String status,
            Integer ackCode,
            String ackMessage,
            Long ackedAtMs,
            String rawPayload,
            String sourceTopic,
            String traceId) {
        try {
            CommandAck ack = new CommandAck();
            ack.setDeviceId(deviceId);
            ack.setCommandId(commandId);
            ack.setStatus(defaultIfBlank(status, "unknown"));
            ack.setAckCode(ackCode);
            ack.setAckMessage(ackMessage);
            ack.setSourceTopic(sourceTopic);
            ack.setRawPayload(rawPayload);
            ack.setTraceId(traceId);
            ack.setAckedAt(toDateTime(ackedAtMs));
            commandAckRepository.save(ack);

            commandRequestRepository.findByCommandId(commandId).ifPresent(req -> {
                req.setStatus(defaultIfBlank(status, "unknown"));
                req.setAckedAt(toDateTime(ackedAtMs));
                commandRequestRepository.save(req);
            });
        } catch (Exception e) {
            log.warn("[ProtocolPersistence] Failed to persist command ack: commandId={}", commandId, e);
        }
    }

    public void recordIngestAudit(
            String msgType,
            String sourceTopic,
            String deviceId,
            String protocolVersion,
            String traceId,
            String status,
            String errorMessage) {
        try {
            IngestAudit audit = new IngestAudit();
            audit.setMsgType(defaultIfBlank(msgType, "unknown"));
            audit.setSourceTopic(defaultIfBlank(sourceTopic, "unknown"));
            audit.setDeviceId(deviceId);
            audit.setProtocolVersion(protocolVersion);
            audit.setTraceId(traceId);
            audit.setStatus(defaultIfBlank(status, "ok"));
            audit.setErrorMessage(errorMessage);
            ingestAuditRepository.save(audit);
        } catch (Exception e) {
            log.warn("[ProtocolPersistence] Failed to persist ingest audit: topic={}", sourceTopic, e);
        }
    }

    public void recordDeadletter(
            String sourceTopic,
            String deviceId,
            String msgType,
            String reason,
            String rawPayload,
            String traceId) {
        try {
            IngestDeadletter deadletter = new IngestDeadletter();
            deadletter.setSourceTopic(defaultIfBlank(sourceTopic, "unknown"));
            deadletter.setDeviceId(deviceId);
            deadletter.setMsgType(defaultIfBlank(msgType, "unknown"));
            deadletter.setReason(defaultIfBlank(reason, "unknown_error"));
            deadletter.setRawPayload(rawPayload);
            deadletter.setTraceId(traceId);
            ingestDeadletterRepository.save(deadletter);
        } catch (Exception e) {
            log.warn("[ProtocolPersistence] Failed to persist deadletter: topic={}", sourceTopic, e);
        }
    }

    private static LocalDateTime toDateTime(Long epochMs) {
        if (epochMs == null || epochMs <= 0) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
