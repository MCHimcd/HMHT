package ww.wangnong.service;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Purpose: 定义告警接入、查询与处理契约。
 * Responsibilities: 统一告警入库模型、分页查询结果和处理动作接口，屏蔽控制器与桥接层对持久化细节的感知。
 * Flow: MqttBridge/Controller 构造 AlertInfo 或查询条件 -> 实现类读写仓库 -> 返回面向前端的
 * snake_case 结果。
 * Dependencies: 告警实体与仓库、Jackson snake_case 映射。
 * Input: 告警信息对象、查询 Map、告警处理动作。
 * Output: 告警分页结果或处理完成状态。
 */
public interface AlertService {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record AlertInfo(String alertId, String nodeId, String type, String typeName,
            String level, String message, String source, String sourceName,
            boolean isHandled, Long handleTime, long timestamp) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record AlertListResult(int total, int unhandledCount,
            int handledRecentCount, List<AlertInfo> items) {
    }

    void addAlert(AlertInfo info);

    AlertListResult queryAlerts(Map<String, Object> query);

    void handleAlert(String alertId, String action);
}
