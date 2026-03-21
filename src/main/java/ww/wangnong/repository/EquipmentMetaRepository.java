package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.EquipmentMeta;

public interface EquipmentMetaRepository extends JpaRepository<EquipmentMeta, Long> {

    List<EquipmentMeta> findByDeviceId(String deviceId);
}
