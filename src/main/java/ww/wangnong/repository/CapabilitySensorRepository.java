package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.CapabilitySensor;

/**
 * Purpose: 查询和维护设备 capability 中的传感器能力明细。
 * Responsibilities: 按 deviceId + capabilityVersion 读取或删除整组 sensor 明细。
 * Flow: capability 快照写入时先删后写；NodeService/DeviceService 查询时按版本读取。
 * Dependencies: CapabilitySensor 实体。
 * Output: 传感器能力明细列表。
 */
public interface CapabilitySensorRepository extends JpaRepository<CapabilitySensor, Long> {

    List<CapabilitySensor> findByDeviceIdAndCapabilityVersionOrderBySensorKey(String deviceId,
            String capabilityVersion);

    void deleteByDeviceIdAndCapabilityVersion(String deviceId, String capabilityVersion);
}
