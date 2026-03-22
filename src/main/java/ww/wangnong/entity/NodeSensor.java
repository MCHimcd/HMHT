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
@Table(name = "node_sensor", uniqueConstraints = @UniqueConstraint(columnNames = {"node_id", "sensor_key"}))
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
