package ww.wangnong.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ww.wangnong.entity.OperationLog;

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
    Page<OperationLog> findFiltered(
            @Param("operationType") String operationType,
            @Param("targetId") String targetId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
}
