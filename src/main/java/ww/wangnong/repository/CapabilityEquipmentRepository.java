package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.CapabilityEquipment;

public interface CapabilityEquipmentRepository extends JpaRepository<CapabilityEquipment, Long> {

    List<CapabilityEquipment> findByDeviceIdAndCapabilityVersionOrderByEquipmentKey(String deviceId,
            String capabilityVersion);

    void deleteByDeviceIdAndCapabilityVersion(String deviceId, String capabilityVersion);
}
