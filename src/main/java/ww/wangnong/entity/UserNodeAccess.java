package ww.wangnong.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * Purpose: 记录用户对节点的显式授权关系。
 * Responsibilities: 维护 user_id 与 node_id 的唯一授权映射，补充“节点拥有者”之外的访问权限。
 * Flow: 管理员授权时写入或重建 -> NodeService 列表/详情校验时读取。
 * Dependencies: user_node_access 表、AdminService、NodeService。
 * Input: 用户标识与节点标识。
 * Output: 节点访问授权映射。
 */
@Entity
@Table(name = "user_node_access", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "node_id" }))
@Getter
@Setter
public class UserNodeAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "node_id", nullable = false, length = 50)
    private String nodeId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
