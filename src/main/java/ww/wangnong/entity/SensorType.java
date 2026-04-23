package ww.wangnong.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Purpose: 定义全局传感器类型字典。
 * Responsibilities: 为 sensorKey 提供名称、单位、图标和配色元数据，供节点元数据组装使用。
 * Flow: 种子数据或迁移初始化 -> NodeService 查询并映射到前端 sensor meta。
 * Dependencies: sensor_type 表、NodeService、课程版种子数据。
 * Input: 传感器类型定义。
 * Output: 传感器元数据模板。
 */
@Entity
@Table(name = "sensor_type")
@Getter
@Setter
public class SensorType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_key", nullable = false, unique = true, length = 50)
    private String sensorKey;

    @Column(length = 100)
    private String name;

    @Column(length = 20)
    private String unit;

    @Column(length = 50)
    private String icon;

    @Column(name = "badge_color", length = 20)
    private String badgeColor;

    @Column(name = "bg_color", length = 20)
    private String bgColor;
}
