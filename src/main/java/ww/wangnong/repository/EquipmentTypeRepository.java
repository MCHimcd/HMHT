package ww.wangnong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.EquipmentType;

public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, Long> {

    Optional<EquipmentType> findByEquipmentKey(String equipmentKey);
}
