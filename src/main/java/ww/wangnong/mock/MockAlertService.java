package ww.wangnong.mock;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import ww.wangnong.entity.Alert;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.AlertRepository;
import ww.wangnong.service.AlertService;
import ww.wangnong.service.OperationLogService;

@Service
public class MockAlertService implements AlertService {

    private static final int MAX_SIZE = 200;
    private final AlertRepository alertRepo;
    private final OperationLogService opLog;

    public MockAlertService(AlertRepository alertRepo, OperationLogService opLog) {
        this.alertRepo = alertRepo;
        this.opLog = opLog;
    }

    @Override
    public void addAlert(AlertInfo info) {
        long count = alertRepo.count();
        if (count >= MAX_SIZE) {
            alertRepo.findAll(Sort.by(Sort.Direction.ASC, "createdAt"))
                    .stream().limit(count - MAX_SIZE + 1)
                    .forEach(alertRepo::delete);
        }

        Alert entity = new Alert();
        entity.setAlertId(info.alertId());
        entity.setNodeId(info.nodeId());
        entity.setType(info.type());
        entity.setTypeName(info.typeName());
        entity.setLevel(info.level());
        entity.setMessage(info.message());
        entity.setSource(info.source());
        entity.setSourceName(info.sourceName());
        entity.setIsHandled(info.isHandled());
        entity.setAlertTime(LocalDateTime.ofInstant(
                Instant.ofEpochMilli(info.timestamp()), ZoneId.systemDefault()));
        if (info.isHandled() && info.handleTime() != null) {
            entity.setHandleTime(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(info.handleTime()), ZoneId.systemDefault()));
        }
        alertRepo.save(entity);
    }

    @Override
    public AlertListResult queryAlerts(Map<String, Object> query) {
        String scene = str(query.getOrDefault("scene", "")).toLowerCase();
        int page = Math.max(1, toInt(query.getOrDefault("page", 1), 1));
        int size = Math.max(1, toInt(query.getOrDefault("pageSize", 20), 20));

        List<Alert> all = alertRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        List<AlertInfo> items;
        if ("startup".equals(scene)) {
            List<AlertInfo> unhandled = all.stream()
                    .filter(a -> !a.getIsHandled()).map(this::toInfo).toList();
            List<AlertInfo> handled = all.stream()
                    .filter(Alert::getIsHandled).limit(10).map(this::toInfo).toList();
            items = new ArrayList<>(unhandled);
            items.addAll(handled);
        } else {
            items = all.stream().map(this::toInfo)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        String filterType = str(query.getOrDefault("filter_type", ""));
        if (!filterType.isEmpty() && !"all".equalsIgnoreCase(filterType)) {
            items = items.stream().filter(a -> a.type().equals(filterType)).toList();
        }

        String filterStatus = str(query.getOrDefault("filter_status", ""));
        if ("handled".equalsIgnoreCase(filterStatus)) {
            items = items.stream().filter(AlertInfo::isHandled).toList();
        } else if ("unhandled".equalsIgnoreCase(filterStatus)) {
            items = items.stream().filter(a -> !a.isHandled()).toList();
        }

        int total = items.size();
        int unhandledCount = (int) items.stream().filter(a -> !a.isHandled()).count();
        int handledRecentCount = (int) Math.min(10,
                items.stream().filter(AlertInfo::isHandled).count());
        int start = Math.min((page - 1) * size, total);
        int end = Math.min(start + size, total);

        return new AlertListResult(total, unhandledCount, handledRecentCount,
                items.subList(start, end));
    }

    @Override
    public void handleAlert(String alertId, String action) {
        Alert alert = alertRepo.findByAlertId(alertId)
                .orElseThrow(() -> new BizException(404, "告警不存在"));
        alert.setIsHandled(true);
        alert.setHandleTime(LocalDateTime.now());
        alertRepo.save(alert);
        opLog.log("ALERT_HANDLE", null, null, "alert", alertId, action);
    }

    private AlertInfo toInfo(Alert a) {
        long ts = a.getAlertTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Long ht = a.getHandleTime() != null
                ? a.getHandleTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : null;
        return new AlertInfo(a.getAlertId(), a.getNodeId(), a.getType(), a.getTypeName(),
                a.getLevel(), a.getMessage(), a.getSource(), a.getSourceName(),
                a.getIsHandled(), ht, ts);
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static int toInt(Object v, int def) {
        if (v instanceof Number n)
            return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
