package ww.wangnong.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.IngestAudit;

public interface IngestAuditRepository extends JpaRepository<IngestAudit, Long> {
}
