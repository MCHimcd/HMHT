package ww.wangnong.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "alerts")
@Getter
@Setter
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false, unique = true, length = 50)
    private String alertId;

    @Column(name = "node_id", nullable = false, length = 50)
    private String nodeId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "type_name", length = 100)
    private String typeName;

    @Column(nullable = false, length = 20)
    private String level;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 50)
    private String source;

    @Column(name = "source_name", length = 100)
    private String sourceName;

    @Column(name = "is_handled", nullable = false)
    private Boolean isHandled = false;

    @Column(name = "handle_time")
    private LocalDateTime handleTime;

    @Column(name = "alert_time", nullable = false)
    private LocalDateTime alertTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
