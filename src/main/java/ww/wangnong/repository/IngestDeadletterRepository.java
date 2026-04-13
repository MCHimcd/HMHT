package ww.wangnong.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.IngestDeadletter;

/**
 * Purpose: 保存桥接层死信消息。
 * Responsibilities: 提供 ingest_deadletter 的基础持久化能力。
 * Flow: ProtocolPersistenceService 在不可恢复错误时写入。
 * Dependencies: IngestDeadletter 实体。
 * Output: 死信记录。
 */
public interface IngestDeadletterRepository extends JpaRepository<IngestDeadletter, Long> {
}
