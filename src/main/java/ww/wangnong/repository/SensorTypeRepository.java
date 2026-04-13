package ww.wangnong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.SensorType;

/**
 * Purpose: 提供全局传感器类型字典查询。
 * Responsibilities: 通过 sensorKey 读取传感器展示元数据。
 * Flow: NodeService 与种子流程按 key 查询类型定义。
 * Dependencies: SensorType 实体。
 * Output: 传感器类型定义。
 */
public interface SensorTypeRepository extends JpaRepository<SensorType, Long> {

    Optional<SensorType> findBySensorKey(String sensorKey);
}
