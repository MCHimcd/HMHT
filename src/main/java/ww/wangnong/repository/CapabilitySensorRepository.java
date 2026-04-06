package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.CapabilitySensor;

public interface CapabilitySensorRepository extends JpaRepository<CapabilitySensor, Long> {

    List<CapabilitySensor> findByDeviceIdAndCapabilityVersionOrderBySensorKey(String deviceId, String capabilityVersion);

    void deleteByDeviceIdAndCapabilityVersion(String deviceId, String capabilityVersion);
}
