package ww.wangnong.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.SensorService;

/**
 * Purpose: 暴露传感器历史与设备统计查询接口。
 * Responsibilities: 兼容不同前端字段命名，从混合请求结构中归一化 nodeId 与设备 key 后调用 SensorService。
 * Flow: 历史接口直接透传 nodeId/metric/range；统计接口先解析 body 与 query 嵌套字段，再执行查询。
 * Dependencies: SensorService、ApiResponse。
 * Input: 节点标识、指标名、时间范围，以及带兼容字段的设备统计查询体。
 * Output: 传感器历史序列或设备统计结果。
 */
@RestController
public class SensorController {

    private static final String DEFAULT_COURSE_NODE_ID = "SIM_Node_01";

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @GetMapping("/v1/sensors/{nodeId}/history")
    public ApiResponse<?> getSensorHistory(
            @PathVariable String nodeId,
            @RequestParam(defaultValue = "temperature") String metric,
            @RequestParam(defaultValue = "24h") String range) {
        return ApiResponse.ok(sensorService.getSensorHistory(nodeId, metric, range));
    }

    @PostMapping("/v1/equipment/stats")
    public ApiResponse<?> getEquipmentStats(@RequestBody Map<String, Object> body) {
        // Reason: 该接口同时兼容 node_id/device_id、equipment_key/device_type 等历史字段，service
        // 层只接受归一化后的主键参数。
        @SuppressWarnings("unchecked")
        Map<String, Object> query = body.get("query") instanceof Map<?, ?>
                ? (Map<String, Object>) body.get("query")
                : Map.of();

        String nodeId = firstNonBlank(
                str(body.get("node_id")),
                str(query.get("node_id")),
                str(body.get("device_id")),
                str(query.get("device_id")),
                DEFAULT_COURSE_NODE_ID);

        String key = firstNonBlank(
                str(body.get("equipment_key")),
                str(query.get("equipment_key")),
                str(query.get("equipment_type")),
                str(query.get("device_type")),
                "water_pump");

        return ApiResponse.ok(sensorService.getEquipmentStats(nodeId, key));
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank())
                return v;
        }
        return "";
    }
}
