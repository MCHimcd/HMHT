package ww.wangnong.service;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public interface AlertService {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record AlertInfo(String alertId, String deviceId, String type, String typeName,
                     String level, String message, String source, String sourceName,
                     boolean isHandled, Long handleTime, long timestamp) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record AlertListResult(int total, int unhandledCount,
                           int handledRecentCount, List<AlertInfo> items) {}

    void addAlert(AlertInfo info);

    AlertListResult queryAlerts(Map<String, Object> query);

    void handleAlert(String alertId, String action);
}
