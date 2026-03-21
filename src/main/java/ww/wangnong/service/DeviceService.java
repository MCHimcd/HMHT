package ww.wangnong.service;

import java.util.List;
import java.util.Map;

public interface DeviceService {

    record DeviceInfo(String id, String name, String status) {}

    record DeviceDetail(String id, String name, String status,
                        Map<String, Object> deviceState) {}

    record SensorMetaInfo(String key, String name, String unit, String icon,
                          String badgeColor, String bgColor,
                          Double warningMin, Double warningMax) {}

    record EquipmentMetaInfo(String key, String name, String icon, boolean controllable) {}

    List<DeviceInfo> listDevices();

    DeviceDetail getDevice(String deviceId);

    List<SensorMetaInfo> getSensorMeta(String deviceId);

    List<EquipmentMetaInfo> getEquipmentMeta(String deviceId);
}
