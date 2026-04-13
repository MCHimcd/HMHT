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
 * Purpose: 定义全局设备类型字典。
 * Responsibilities: 为设备 key 提供名称、图标、可控性和 UI 控件类型等元数据。
 * Flow: mock 种子或迁移初始化类型 -> NodeService/前端元数据查询按 equipmentKey 读取。
 * Dependencies: equipment_type 表、NodeService、MockDataLifecycle。
 * Input: 设备类型定义。
 * Output: 设备元数据模板。
 */
@Entity
@Table(name = "equipment_type")
@Getter
@Setter
public class EquipmentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_key", nullable = false, unique = true, length = 50)
    private String equipmentKey;

    @Column(length = 100)
    private String name;

    @Column(length = 50)
    private String icon;

    @Column(nullable = false)
    private Boolean controllable = true;

    /**
     * UI control type: "toggle" (default on/off switch) or "slider" (range 0-100)
     */
    @Column(name = "control_type", length = 20)
    private String controlType = "toggle";
}
