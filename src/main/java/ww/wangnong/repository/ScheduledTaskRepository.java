package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.ScheduledTask;

/**
 * Purpose: 提供定时任务定义的查询。
 * Responsibilities: 支持按用户、启用状态和节点维度读取任务。
 * Flow: ScheduledTaskService 管理任务；执行器按 enabled 查询待执行任务。
 * Dependencies: ScheduledTask 实体。
 * Output: 任务列表。
 */
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long> {

    List<ScheduledTask> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ScheduledTask> findByEnabledTrue();

    List<ScheduledTask> findByNodeId(String nodeId);
}
