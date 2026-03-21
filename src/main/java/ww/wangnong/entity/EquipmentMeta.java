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

@Entity
@Table(name = "equipment_meta", uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "equipment_key"}))
@Getter
@Setter
public class EquipmentMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "equipment_key", nullable = false, length = 50)
    private String equipmentKey;

    @Column(length = 100)
    private String name;

    @Column(length = 50)
    private String icon;

    @Column(nullable = false)
    private Boolean controllable = true;
}
