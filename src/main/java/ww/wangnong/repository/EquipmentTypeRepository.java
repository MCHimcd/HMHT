package ww.wangnong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.EquipmentType;

/**
 * Purpose: 提供全局设备类型字典查询。
 * Responsibilities: 通过 equipmentKey 读取设备展示与控制元数据。
 * Flow: NodeService 和种子流程按 key 查询字典项。
 * Dependencies: EquipmentType 实体。
 * Output: 设备类型定义。
 */
public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, Long> {

    Optional<EquipmentType> findByEquipmentKey(String equipmentKey);
}
