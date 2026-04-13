package ww.wangnong.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ww.wangnong.entity.OperationLog;

/**
 * Purpose: 提供操作审计日志分页与筛选查询。
 * Responsibilities: 按时间倒序分页读取日志，并支持类型、目标和时间区间组合过滤。
 * Flow: OperationLogService 写入/查询时使用本仓库。
 * Dependencies: OperationLog 实体、Spring Data 分页与 JPQL。
 * Output: 审计日志分页结果或最近日志列表。
 */
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

        Page<OperationLog> findByOrderByCreatedAtDesc(Pageable pageable);

        Page<OperationLog> findByOperationTypeOrderByCreatedAtDesc(String operationType, Pageable pageable);

        Page<OperationLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

        List<OperationLog> findTop50ByOrderByCreatedAtDesc();

        @Query("SELECT o FROM OperationLog o WHERE "
                        + "(:operationType IS NULL OR o.operationType = :operationType) "
                        + "AND (:targetId IS NULL OR o.targetId = :targetId) "
                        + "AND (:startTime IS NULL OR o.createdAt >= :startTime) "
                        + "AND (:endTime IS NULL OR o.createdAt <= :endTime) "
                        + "ORDER BY o.createdAt DESC")
        // Boundary: 该查询使用可空条件拼接，调用方传 null 才表示“不筛选”，空字符串应在 service 层先归一化。
        Page<OperationLog> findFiltered(
                        @Param("operationType") String operationType,
                        @Param("targetId") String targetId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime,
                        Pageable pageable);
}
