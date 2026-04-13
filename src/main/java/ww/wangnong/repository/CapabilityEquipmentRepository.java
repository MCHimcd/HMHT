package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.CapabilityEquipment;

/**
 * Purpose: 查询和维护设备 capability 中的设备控制项明细。
 * Responsibilities: 按 deviceId + capabilityVersion 读取或删除整组 equipment 明细。
 * Flow: ProtocolPersistenceService 覆盖写入；DeviceService/NodeService 按版本读取。
 * Dependencies: CapabilityEquipment 实体。
 * Output: 设备能力明细列表。
 */
public interface CapabilityEquipmentRepository extends JpaRepository<CapabilityEquipment, Long> {

    List<CapabilityEquipment> findByDeviceIdAndCapabilityVersionOrderByEquipmentKey(String deviceId,
            String capabilityVersion);

    void deleteByDeviceIdAndCapabilityVersion(String deviceId, String capabilityVersion);
}
