package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.CommandRetryLog;

public interface CommandRetryLogRepository extends JpaRepository<CommandRetryLog, Long> {

    long countByCommandId(String commandId);

    List<CommandRetryLog> findByCommandIdOrderByCreatedAtDesc(String commandId);
}
