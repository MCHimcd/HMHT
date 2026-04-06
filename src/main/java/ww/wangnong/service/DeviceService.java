package ww.wangnong.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface DeviceService {

    record SensorCapability(
            String sensorKey,
            String dataType,
            String unit,
            Double minValue,
            Double maxValue,
            Boolean requiredField) {
    }

    record EquipmentCapability(
            String equipmentKey,
            Boolean controllable,
            List<String> actions) {
    }

    record DeviceCapabilityResult(
            String deviceId,
            String capabilityVersion,
            String checksum,
            LocalDateTime reportedAt,
            List<SensorCapability> sensors,
            List<EquipmentCapability> equipment) {
    }

    record DeviceShadowResult(
            String deviceId,
            Map<String, Object> shadowState,
            LocalDateTime updatedAt) {
    }

    DeviceCapabilityResult getDeviceCapability(String deviceId);

    DeviceShadowResult getDeviceShadow(String deviceId);
}
