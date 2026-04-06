package ww.wangnong.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.IngestDeadletter;

public interface IngestDeadletterRepository extends JpaRepository<IngestDeadletter, Long> {
}
