package ww.wangnong.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * Purpose: 保存农业节点主档案。
 * Responsibilities: 记录节点唯一标识、显示名、所属用户以及基础在线/智能模式状态。
 * Flow: 管理员或种子流程创建节点 -> NodeService/控制器按 nodeId 查询 -> 桥接或业务逻辑更新状态字段。
 * Dependencies: nodes 表、User 实体、NodeService。
 * Input: 节点元数据和归属关系。
 * Output: 节点主记录。
 */
@Entity
@Table(name = "nodes")
@Getter
@Setter
public class Node {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false, unique = true, length = 50)
    private String nodeId;

    @Column(name = "node_name", length = 100)
    private String nodeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Boolean online = false;

    @Column(name = "smart_mode", nullable = false)
    private Boolean smartMode = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
