package ww.wangnong.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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
