package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.CommandAck;

/**
 * Purpose: 提供命令 ACK 回执流水查询。
 * Responsibilities: 按 commandId 读取回执历史，并保持最近回执优先。
 * Flow: ProtocolPersistenceService 写入；命令闭环查询按 commandId 读取。
 * Dependencies: CommandAck 实体。
 * Output: 命令回执列表。
 */
public interface CommandAckRepository extends JpaRepository<CommandAck, Long> {

    List<CommandAck> findByCommandIdOrderByCreatedAtDesc(String commandId);
}
