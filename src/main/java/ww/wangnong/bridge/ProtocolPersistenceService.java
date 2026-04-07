package ww.wangnong.bridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ww.wangnong.config.condition.ConditionalOnBridgeMode;
import ww.wangnong.entity.CommandAck;
import ww.wangnong.entity.CommandRequest;
import ww.wangnong.entity.CapabilityEquipment;
import ww.wangnong.entity.CapabilitySensor;
import ww.wangnong.entity.DeviceCapability;
import ww.wangnong.entity.DeviceProfile;
import ww.wangnong.entity.DeviceShadow;
import ww.wangnong.entity.IngestAudit;
import ww.wangnong.entity.IngestDeadletter;
import ww.wangnong.repository.CapabilityEquipmentRepository;
import ww.wangnong.repository.CapabilitySensorRepository;
import ww.wangnong.repository.CommandAckRepository;
import ww.wangnong.repository.CommandRequestRepository;
import ww.wangnong.repository.DeviceCapabilityRepository;
import ww.wangnong.repository.DeviceProfileRepository;
import ww.wangnong.repository.DeviceShadowRepository;
import ww.wangnong.repository.IngestAuditRepository;
import ww.wangnong.repository.IngestDeadletterRepository;

/**
 * 协议控制面持久化服务，处理命令、ACK、能力、影子等数据的持久化。
 * 在桥接模式下激活（bridge/hardware/full）。
 */
@Service
@ConditionalOnBridgeMode
public class ProtocolPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ProtocolPersistenceService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final CommandRequestRepository commandRequestRepository;
    private final CommandAckRepository commandAckRepository;
    private final IngestAuditRepository ingestAuditRepository;
    private final IngestDeadletterRepository ingestDeadletterRepository;
    private final DeviceShadowRepository deviceShadowRepository;
    private final DeviceProfileRepository deviceProfileRepository;
    private final DeviceCapabilityRepository deviceCapabilityRepository;
    private final CapabilitySensorRepository capabilitySensorRepository;
    private final CapabilityEquipmentRepository capabilityEquipmentRepository;

    public ProtocolPersistenceService(
            CommandRequestRepository commandRequestRepository,
            CommandAckRepository commandAckRepository,
            IngestAuditRepository ingestAuditRepository,
            IngestDeadletterRepository ingestDeadletterRepository,
            DeviceShadowRepository deviceShadowRepository,
            DeviceProfileRepository deviceProfileRepository,
            DeviceCapabilityRepository deviceCapabilityRepository,
            CapabilitySensorRepository capabilitySensorRepository,
            CapabilityEquipmentRepository capabilityEquipmentRepository) {
        this.commandRequestRepository = commandRequestRepository;
        this.commandAckRepository = commandAckRepository;
        this.ingestAuditRepository = ingestAuditRepository;
        this.ingestDeadletterRepository = ingestDeadletterRepository;
        this.deviceShadowRepository = deviceShadowRepository;
        this.deviceProfileRepository = deviceProfileRepository;
        this.deviceCapabilityRepository = deviceCapabilityRepository;
        this.capabilitySensorRepository = capabilitySensorRepository;
        this.capabilityEquipmentRepository = capabilityEquipmentRepository;
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
            request.setRetryCount(0);
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

            upsertDeviceShadow(deviceId, commandId, status, ackCode, ackMessage, traceId, ackedAtMs, rawPayload);
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

    @SuppressWarnings("unchecked")
    public void recordCapabilitySnapshot(
            String deviceId,
            String capabilityVersion,
            String checksum,
            Map<String, Object> sensors,
            Map<String, Object> equipments,
            Long reportedAtMs,
            String sourceTopic,
            String rawPayload,
            String traceId) {
        try {
            String safeDeviceId = defaultIfBlank(deviceId, "unknown");
            String safeVersion = defaultIfBlank(capabilityVersion, "2.0");
            LocalDateTime reportedAt = toDateTime(reportedAtMs);

            DeviceProfile profile = deviceProfileRepository.findByDeviceId(safeDeviceId).orElseGet(DeviceProfile::new);
            profile.setDeviceId(safeDeviceId);
            profile.setOnline(true);
            profile.setLastSeenAt(reportedAt);
            deviceProfileRepository.save(profile);

            DeviceCapability capability = new DeviceCapability();
            capability.setDeviceId(safeDeviceId);
            capability.setCapabilityVersion(safeVersion);
            capability.setChecksum(checksum);
            capability.setReportedAt(reportedAt);
            deviceCapabilityRepository.save(capability);

            capabilitySensorRepository.deleteByDeviceIdAndCapabilityVersion(safeDeviceId, safeVersion);
            capabilityEquipmentRepository.deleteByDeviceIdAndCapabilityVersion(safeDeviceId, safeVersion);

            for (var entry : sensors.entrySet()) {
                CapabilitySensor sensor = new CapabilitySensor();
                sensor.setDeviceId(safeDeviceId);
                sensor.setCapabilityVersion(safeVersion);
                sensor.setSensorKey(entry.getKey());

                Object raw = entry.getValue();
                if (raw instanceof Map<?, ?> detailMap) {
                    Map<String, Object> detail = (Map<String, Object>) detailMap;
                    sensor.setDataType(defaultIfBlank(str(detail.get("data_type")), inferDataType(detail.get("value"))));
                    sensor.setUnit(str(detail.get("unit")));
                    sensor.setMinValue(toDouble(firstNonNull(detail.get("min"), detail.get("min_value"))));
                    sensor.setMaxValue(toDouble(firstNonNull(detail.get("max"), detail.get("max_value"))));
                    sensor.setRequiredField(toBoolean(detail.get("required_field"), false));
                } else {
                    sensor.setDataType(inferDataType(raw));
                    sensor.setRequiredField(false);
                }
                capabilitySensorRepository.save(sensor);
            }

            for (var entry : equipments.entrySet()) {
                CapabilityEquipment equipment = new CapabilityEquipment();
                equipment.setDeviceId(safeDeviceId);
                equipment.setCapabilityVersion(safeVersion);
                equipment.setEquipmentKey(entry.getKey());

                Object raw = entry.getValue();
                if (raw instanceof Map<?, ?> detailMap) {
                    Map<String, Object> detail = (Map<String, Object>) detailMap;
                    equipment.setControllable(toBoolean(detail.get("controllable"), true));
                    equipment.setActionsJson(toActionsJson(detail.get("actions")));
                } else {
                    equipment.setControllable(true);
                    equipment.setActionsJson("[]");
                }
                capabilityEquipmentRepository.save(equipment);
            }

            recordIngestAudit(
                    "capability",
                    sourceTopic,
                    safeDeviceId,
                    safeVersion,
                    traceId,
                    "stored",
                    null);
        } catch (Exception e) {
            log.warn("[ProtocolPersistence] Failed to persist capability snapshot: deviceId={}", deviceId, e);
            recordDeadletter(
                    sourceTopic,
                    defaultIfBlank(deviceId, ""),
                    "capability",
                    e.getMessage(),
                    rawPayload,
                    traceId);
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

    private static String defaultIfBlank(String value) {
        return defaultIfBlank(value, "");
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private static String inferDataType(Object value) {
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        return "string";
    }

    private static Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private static Boolean toBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(value);
        if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
            return true;
        }
        if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
            return false;
        }
        return fallback;
    }

    private static String toActionsJson(Object actions) {
        try {
            if (actions == null) {
                return "[]";
            }
            if (actions instanceof List<?> || actions.getClass().isArray()) {
                return MAPPER.writeValueAsString(actions);
            }
            return MAPPER.writeValueAsString(List.of(String.valueOf(actions)));
        } catch (Exception e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private void upsertDeviceShadow(
            String deviceId,
            String commandId,
            String status,
            Integer ackCode,
            String ackMessage,
            String traceId,
            Long ackedAtMs,
            String rawPayload) {
        try {
            DeviceShadow shadow = deviceShadowRepository.findByDeviceId(deviceId).orElseGet(() -> {
                DeviceShadow created = new DeviceShadow();
                created.setDeviceId(deviceId);
                return created;
            });

            Map<String, Object> shadowState = new LinkedHashMap<>();
            shadowState.put("last_command_id", commandId);
            shadowState.put("status", defaultIfBlank(status, "unknown"));
            shadowState.put("acked_at", toDateTime(ackedAtMs).toString());
            if (ackCode != null) {
                shadowState.put("ack_code", ackCode);
            }
            if (ackMessage != null && !ackMessage.isBlank()) {
                shadowState.put("ack_message", ackMessage);
            }
            if (traceId != null && !traceId.isBlank()) {
                shadowState.put("trace_id", traceId);
            }

            try {
                Map<String, Object> envelope = MAPPER.readValue(rawPayload, MAP_TYPE);
                Object payloadObj = envelope.get("payload");
                if (payloadObj instanceof Map<?, ?> payload) {
                    shadowState.put("ack_payload", payload);
                }
            } catch (Exception ignored) {
                if (rawPayload != null && !rawPayload.isBlank()) {
                    shadowState.put("ack_raw", rawPayload);
                }
            }

            shadow.setShadowJson(MAPPER.writeValueAsString(shadowState));
            deviceShadowRepository.save(shadow);
        } catch (Exception e) {
            log.warn("[ProtocolPersistence] Failed to upsert device shadow: deviceId={}", deviceId, e);
        }
    }
}
