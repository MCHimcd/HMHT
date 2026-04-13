package ww.wangnong.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.IngestAudit;

/**
 * Purpose: 保存桥接层消息接入审计流水。
 * Responsibilities: 提供 ingest_audit 的基础持久化能力。
 * Flow: ProtocolPersistenceService 写入处理结果。
 * Dependencies: IngestAudit 实体。
 * Output: 接入审计记录。
 */
public interface IngestAuditRepository extends JpaRepository<IngestAudit, Long> {
}
