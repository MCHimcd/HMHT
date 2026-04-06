package ww.wangnong.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "device_capability", indexes = {
        @Index(name = "idx_device_capability_device", columnList = "device_id,created_at")
})
@Getter
@Setter
public class DeviceCapability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "capability_version", nullable = false, length = 20)
    private String capabilityVersion;

    @Column(length = 80)
    private String checksum;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
