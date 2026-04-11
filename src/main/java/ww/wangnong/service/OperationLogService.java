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
        return LocalDate.parse(dateStr).atTime(LocalTime.MAX);
    }
}
