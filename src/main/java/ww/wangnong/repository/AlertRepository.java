package ww.wangnong.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.Alert;

/**
 * Purpose: 提供告警实体的持久化与常用查询。
 * Responsibilities: 支持按 alertId、节点、处理状态和时间倒序读取告警。
 * Flow: AlertService 写入/查询时调用本仓库。
 * Dependencies: Alert 实体、Spring Data JPA。
 * Output: 告警实体或分页结果。
 */
public interface AlertRepository extends JpaRepository<Alert, Long> {

    Optional<Alert> findByAlertId(String alertId);

    Page<Alert> findByNodeIdOrderByCreatedAtDesc(String nodeId, Pageable pageable);

    Page<Alert> findByIsHandledOrderByCreatedAtDesc(boolean isHandled, Pageable pageable);

    List<Alert> findTop10ByIsHandledOrderByCreatedAtDesc(boolean isHandled);
}
