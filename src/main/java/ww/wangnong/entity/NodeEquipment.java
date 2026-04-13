package ww.wangnong.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Purpose: 记录节点与设备类型的静态绑定关系。
 * Responsibilities: 维护 node_id 与 equipment_key 的唯一组合，供旧版元数据查询和 mock 种子使用。
 * Flow: MockDataLifecycle 或管理逻辑写入 -> NodeService 在无 capability 快照时回退读取。
 * Dependencies: node_equipment 表。
 * Input: 节点标识与设备 key。
 * Output: 节点设备绑定关系。
 */
@Entity
@Table(name = "node_equipment", uniqueConstraints = @UniqueConstraint(columnNames = { "node_id", "equipment_key" }))
@Getter
@Setter
public class NodeEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false, length = 50)
    private String nodeId;

    @Column(name = "equipment_key", nullable = false, length = 50)
    private String equipmentKey;
}
