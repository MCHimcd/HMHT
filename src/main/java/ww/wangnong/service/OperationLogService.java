package ww.wangnong.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import ww.wangnong.entity.OperationLog;
import ww.wangnong.repository.OperationLogRepository;

/**
 * Purpose: 负责后台操作审计日志的写入与分页查询。
 * Responsibilities: 将业务动作写入 operation_logs，并提供分页、用户维度和时间区间筛选视图。
 * Flow: 业务服务调用 log 写入审计；控制器调用 list/listFiltered/listByUser 获取分页结果。
 * Dependencies: OperationLogRepository、Spring Data 分页。
 * Input: 操作类型、操作人、目标信息、分页参数、时间筛选参数。
 * Output: 审计记录分页结果或持久化后的日志副作用。
 */
@Service
public class OperationLogService {

    private final OperationLogRepository repo;

    public OperationLogService(OperationLogRepository repo) {
        this.repo = repo;
    }

    public void log(String operationType, Long userId, String userName,
            String targetType, String targetId, String detail) {
        OperationLog entry = new OperationLog();
        entry.setUserId(userId);
        entry.setUserName(userName);
        entry.setOperationType(operationType);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setDetail(detail);
        repo.save(entry);
    }

    public record LogItem(long id, Long userId, String userName, String operationType,
            String targetType, String targetId, String detail,
            String createdAt) {
    }

    public record LogPage(int total, List<LogItem> items) {
    }

    public LogPage list(int page, int size, String filterType) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, Math.min(size, 100)));
        Page<OperationLog> result;
        if (filterType != null && !filterType.isBlank()) {
            result = repo.findByOperationTypeOrderByCreatedAtDesc(filterType, pageable);
        } else {
            result = repo.findByOrderByCreatedAtDesc(pageable);
        }

        List<LogItem> items = result.getContent().stream()
                .map(e -> new LogItem(e.getId(), e.getUserId(), e.getUserName(),
                        e.getOperationType(), e.getTargetType(), e.getTargetId(),
                        e.getDetail(),
                        e.getCreatedAt() != null ? e.getCreatedAt().toString() : null))
                .toList();
        return new LogPage((int) result.getTotalElements(), items);
    }

    public LogPage listFiltered(int page, int size, String filterType, String targetId,
            String startDate, String endDate) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, Math.min(size, 100)));

        String opType = (filterType != null && !filterType.isBlank()) ? filterType : null;
        String tid = (targetId != null && !targetId.isBlank()) ? targetId : null;
        LocalDateTime startTime = parseStartDate(startDate);
        LocalDateTime endTime = parseEndDate(endDate);

        Page<OperationLog> result = repo.findFiltered(opType, tid, startTime, endTime, pageable);

        List<LogItem> items = result.getContent().stream()
                .map(e -> new LogItem(e.getId(), e.getUserId(), e.getUserName(),
                        e.getOperationType(), e.getTargetType(), e.getTargetId(),
                        e.getDetail(),
                        e.getCreatedAt() != null ? e.getCreatedAt().toString() : null))
                .toList();
        return new LogPage((int) result.getTotalElements(), items);
    }

    public LogPage listByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, Math.min(size, 100)));
        Page<OperationLog> result = repo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<LogItem> items = result.getContent().stream()
                .map(e -> new LogItem(e.getId(), e.getUserId(), e.getUserName(),
                        e.getOperationType(), e.getTargetType(), e.getTargetId(),
                        e.getDetail(),
                        e.getCreatedAt() != null ? e.getCreatedAt().toString() : null))
                .toList();
        return new LogPage((int) result.getTotalElements(), items);
    }

    private LocalDateTime parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        return LocalDate.parse(dateStr).atStartOfDay();
    }

    private LocalDateTime parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        // Boundary: 结束日期按当天最后一刻闭区间处理，避免前端按日期筛选时漏掉尾日记录。
        return LocalDate.parse(dateStr).atTime(LocalTime.MAX);
    }
}
