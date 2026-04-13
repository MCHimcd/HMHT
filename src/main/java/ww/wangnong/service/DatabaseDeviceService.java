package ww.wangnong.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ww.wangnong.entity.CapabilityEquipment;
import ww.wangnong.entity.DeviceCapability;
import ww.wangnong.entity.DeviceShadow;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.CapabilityEquipmentRepository;
import ww.wangnong.repository.CapabilitySensorRepository;
import ww.wangnong.repository.DeviceCapabilityRepository;
import ww.wangnong.repository.DeviceShadowRepository;

/**
 * Purpose: 从控制面表聚合设备能力与影子查询结果。
 * Responsibilities: 读取最新 capability/shadow 快照，拼装控制器所需 DTO，并在 JSON
 * 反序列化失败时提供可诊断回退值。
 * Flow: 查询最新 DeviceCapability/DeviceShadow -> 关联 capability 明细 -> 解析 JSON 字段 ->
 * 返回 DeviceService record。
 * Dependencies:
 * DeviceCapabilityRepository、Capability*Repository、DeviceShadowRepository、ObjectMapper。
 * Input: deviceId。
 * Output: 设备能力快照或设备影子状态。
 * Boundary: capability 查询始终依赖最新版本号；shadow/action JSON 解析失败时不能中断查询链路。
 */
@Service
public class DatabaseDeviceService implements DeviceService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final DeviceCapabilityRepository deviceCapabilityRepository;
    private final CapabilitySensorRepository capabilitySensorRepository;
    private final CapabilityEquipmentRepository capabilityEquipmentRepository;
    private final DeviceShadowRepository deviceShadowRepository;
    private final ObjectMapper objectMapper;

    public DatabaseDeviceService(
            DeviceCapabilityRepository deviceCapabilityRepository,
            CapabilitySensorRepository capabilitySensorRepository,
            CapabilityEquipmentRepository capabilityEquipmentRepository,
            DeviceShadowRepository deviceShadowRepository) {
        this.deviceCapabilityRepository = deviceCapabilityRepository;
        this.capabilitySensorRepository = capabilitySensorRepository;
        this.capabilityEquipmentRepository = capabilityEquipmentRepository;
        this.deviceShadowRepository = deviceShadowRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public DeviceCapabilityResult getDeviceCapability(String deviceId) {
        DeviceCapability capability = deviceCapabilityRepository
                .findTopByDeviceIdOrderByCreatedAtDesc(deviceId)
                .orElseThrow(() -> new BizException(404, "设备能力不存在"));

        List<SensorCapability> sensors = capabilitySensorRepository
                .findByDeviceIdAndCapabilityVersionOrderBySensorKey(deviceId, capability.getCapabilityVersion())
                .stream()
                .map(sensor -> new SensorCapability(
                        sensor.getSensorKey(),
                        sensor.getDataType(),
                        sensor.getUnit(),
                        sensor.getMinValue(),
                        sensor.getMaxValue(),
                        sensor.getRequiredField()))
                .toList();

        List<EquipmentCapability> equipment = capabilityEquipmentRepository
                .findByDeviceIdAndCapabilityVersionOrderByEquipmentKey(deviceId, capability.getCapabilityVersion())
                .stream()
                .map(this::toEquipmentCapability)
                .toList();

        return new DeviceCapabilityResult(
                capability.getDeviceId(),
                capability.getCapabilityVersion(),
                capability.getChecksum(),
                capability.getReportedAt(),
                sensors,
                equipment);
    }

    @Override
    public DeviceShadowResult getDeviceShadow(String deviceId) {
        DeviceShadow shadow = deviceShadowRepository
                .findByDeviceId(deviceId)
                .orElseThrow(() -> new BizException(404, "设备影子不存在"));

        return new DeviceShadowResult(
                shadow.getDeviceId(),
                parseShadowMap(shadow.getShadowJson()),
                shadow.getUpdatedAt() == null ? LocalDateTime.now() : shadow.getUpdatedAt());
    }

    private EquipmentCapability toEquipmentCapability(CapabilityEquipment equipment) {
        return new EquipmentCapability(
                equipment.getEquipmentKey(),
                equipment.getControllable(),
                parseActions(equipment.getActionsJson()));
    }

    private List<String> parseActions(String actionsJson) {
        if (actionsJson == null || actionsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(actionsJson, STRING_LIST_TYPE);
        } catch (Exception e) {
            // Reason: 历史数据可能写入的是单个字符串而非 JSON 数组，回退为单元素列表以保持接口兼容。
            return List.of(actionsJson);
        }
    }

    private Map<String, Object> parseShadowMap(String shadowJson) {
        if (shadowJson == null || shadowJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(shadowJson, MAP_TYPE);
        } catch (Exception e) {
            // Reason: 影子字段损坏时仍需把原始内容暴露给调用方，避免查询直接失败且便于排查脏数据。
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("raw", shadowJson);
            return fallback;
        }
    }
}
