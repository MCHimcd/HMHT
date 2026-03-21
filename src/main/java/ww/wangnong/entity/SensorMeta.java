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
@Table(name = "sensor_meta", uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "sensor_key"}))
@Getter
@Setter
public class SensorMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "sensor_key", nullable = false, length = 50)
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

    @Column(name = "warning_min")
    private Double warningMin;

    @Column(name = "warning_max")
    private Double warningMax;
}
