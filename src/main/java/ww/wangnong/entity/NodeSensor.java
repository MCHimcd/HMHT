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
 * Purpose: 记录节点与传感器类型的静态绑定及阈值配置。
 * Responsibilities: 保存 node_id 与 sensor_key 的唯一绑定，以及该节点级别的告警阈值。
 * Flow: mock 种子或管理接口写入 -> NodeService 读取用于元数据与阈值展示/更新。
 * Dependencies: node_sensor 表。
 * Input: 节点标识、传感器 key、阈值上下限。
 * Output: 节点传感器绑定关系。
 */
@Entity
@Table(name = "node_sensor", uniqueConstraints = @UniqueConstraint(columnNames = { "node_id", "sensor_key" }))
@Getter
@Setter
public class NodeSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false, length = 50)
    private String nodeId;

    @Column(name = "sensor_key", nullable = false, length = 50)
    private String sensorKey;

    @Column(name = "warning_min")
    private Double warningMin;

    @Column(name = "warning_max")
    private Double warningMax;
}
