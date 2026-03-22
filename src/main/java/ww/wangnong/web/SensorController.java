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

@RestController
public class SensorController {

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
        @SuppressWarnings("unchecked")
        Map<String, Object> query = body.get("query") instanceof Map<?, ?>
                ? (Map<String, Object>) body.get("query")
                : Map.of();

        String nodeId = firstNonBlank(
                str(body.get("node_id")),
                str(query.get("node_id")),
                str(body.get("device_id")),
                str(query.get("device_id")),
                "A03_Node_01");

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
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }
}
