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
