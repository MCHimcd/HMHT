package ww.wangnong.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.CommandLog;

public interface CommandLogRepository extends JpaRepository<CommandLog, Long> {
}
