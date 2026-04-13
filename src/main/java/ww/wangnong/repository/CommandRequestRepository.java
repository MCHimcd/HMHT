package ww.wangnong.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.CommandRequest;

/**
 * Purpose: 提供命令主记录的读写与超时扫描查询。
 * Responsibilities: 按 commandId 精确定位命令生命周期，并按 sentAt 顺序扫描待处理状态集合。
 * Flow: ProtocolPersistenceService 创建/更新命令；CommandAckTracker 周期性查询待重试命令。
 * Dependencies: CommandRequest 实体。
 * Output: 命令主记录或待处理命令列表。
 */
public interface CommandRequestRepository extends JpaRepository<CommandRequest, Long> {

    Optional<CommandRequest> findByCommandId(String commandId);

    // Boundary: 扫描结果按 sentAt 升序返回最早超时的命令，避免新命令长期抢占重试窗口。
    List<CommandRequest> findTop200ByStatusInOrderBySentAtAsc(List<String> statuses);
}
