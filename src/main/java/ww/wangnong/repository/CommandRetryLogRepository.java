package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.CommandRetryLog;

/**
 * Purpose: 提供命令重试日志的统计与历史查询。
 * Responsibilities: 统计某命令已重试次数，并按时间倒序查看重试轨迹。
 * Flow: CommandAckTracker 记录重试；排障或闭环查询读取历史。
 * Dependencies: CommandRetryLog 实体。
 * Output: 重试次数与重试日志列表。
 */
public interface CommandRetryLogRepository extends JpaRepository<CommandRetryLog, Long> {

    long countByCommandId(String commandId);

    List<CommandRetryLog> findByCommandIdOrderByCreatedAtDesc(String commandId);
}
