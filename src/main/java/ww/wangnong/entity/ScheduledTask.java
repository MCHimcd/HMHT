package ww.wangnong.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "scheduled_tasks")
@Getter
@Setter
public class ScheduledTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false, length = 50)
    private String nodeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "equipment_key", nullable = false, length = 50)
    private String equipmentKey;

    /** "turn_on" or "turn_off" */
    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "task_name", length = 100)
    private String taskName;

    /** 0-23 */
    @Column(nullable = false)
    private Integer hour;

    /** 0-59 */
    @Column(nullable = false)
    private Integer minute;

    /**
     * Comma-separated days of week: "1,2,3,4,5" (1=Mon..7=Sun). Empty = every day.
     */
    @Column(name = "repeat_days", length = 20)
    private String repeatDays;

    @Column(nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
