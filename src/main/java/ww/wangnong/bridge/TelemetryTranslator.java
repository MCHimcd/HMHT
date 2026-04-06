package ww.wangnong.bridge;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates telemetry payloads between Python (agri) format and App (wangnong) format.
 * Python → App direction: field renaming + structure normalization.
 * App → Python direction: reverse mapping for commands.
 */
public final class TelemetryTranslator {

    private TelemetryTranslator() {}

    // ── Python environment key → App sensor key ──
    private static final Map<String, String> ENV_PY_TO_APP = Map.of(
            "air_temp", "temperature",
            "air_humidity", "humidity",
            "soil_temp", "soilTemp",
            "soil_moisture", "soilHumidity",
            "soil_ph", "soilPh",
            "light_intensity", "light",
            "co2_ppm", "co2",
            "soil_n", "soilN",
            "soil_p", "soilP",
            "soil_k", "soilK");

    // ── Python equipment_status key → App equipment key (mostly 1:1) ──
    // heater, misting, co2_generator, fertilizer are new equipment from Python side
    // water_pump, led_light, fan, camera are shared

    /**
     * Translate a Python-format telemetry payload to App-format.
     * Input:  { msg_type, device_id, timestamp, data: { environment:{air_temp,...}, equipment_status:{...}, system_mode } }
     * Output: { msg_type, device_id, timestamp, data: { environment:{temperature,...}, equipment:{...}, mode } }
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> pyToApp(Map<String, Object> pyPayload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("msg_type", pyPayload.get("msg_type"));
        result.put("node_id", pyPayload.get("device_id"));
        result.put("timestamp", pyPayload.get("timestamp"));

        Object dataObj = pyPayload.get("data");
        if (!(dataObj instanceof Map<?, ?> pyData)) {
            result.put("data", dataObj);
            return result;
        }

        Map<String, Object> appData = new LinkedHashMap<>();

        // environment
        Object envObj = pyData.get("environment");
        if (envObj instanceof Map<?, ?> pyEnv) {
            Map<String, Object> appEnv = new LinkedHashMap<>();
            for (var entry : ((Map<String, Object>) pyEnv).entrySet()) {
                String appKey = ENV_PY_TO_APP.getOrDefault(entry.getKey(), entry.getKey());
                appEnv.put(appKey, entry.getValue());
            }
            appData.put("environment", appEnv);
        }

        // equipment_status → equipment
        Object eqObj = pyData.get("equipment_status");
        if (eqObj instanceof Map<?, ?>) {
            appData.put("equipment", new LinkedHashMap<>((Map<String, Object>) eqObj));
        }

        // system_mode → mode
        Object modeObj = pyData.get("system_mode");
        if (modeObj != null) {
            appData.put("mode", modeObj);
        }

        result.put("data", appData);
        return result;
    }

    /**
     * Normalize a V2 hardware telemetry envelope to App-format telemetry payload.
     * Input:  { msg_type, protocol_version, device_id, timestamp, trace_id, payload:{sensors,equipments,mode} }
     * Output: { msg_type, node_id, timestamp, data:{environment,equipment,mode,protocol_version,trace_id} }
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> hardwareV2ToApp(Map<String, Object> hwPayload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("msg_type", hwPayload.getOrDefault("msg_type", "telemetry"));
        result.put("node_id", hwPayload.get("device_id"));
        result.put("timestamp", hwPayload.get("timestamp"));

        Object payloadObj = hwPayload.get("payload");
        if (!(payloadObj instanceof Map<?, ?> payload)) {
            result.put("data", payloadObj);
            return result;
        }

        Map<String, Object> appData = new LinkedHashMap<>();

        Object sensorsObj = payload.get("sensors");
        if (sensorsObj instanceof Map<?, ?> sensors) {
            Map<String, Object> appEnv = new LinkedHashMap<>();
            for (var entry : ((Map<String, Object>) sensors).entrySet()) {
                String appKey = ENV_PY_TO_APP.getOrDefault(entry.getKey(), entry.getKey());
                Object normalized = unwrapValue(entry.getValue(), "value");
                if (normalized != null) {
                    appEnv.put(appKey, normalized);
                }
            }
            if (!appEnv.isEmpty()) {
                appData.put("environment", appEnv);
            }
        }

        Object eqObj = payload.get("equipments");
        if (eqObj instanceof Map<?, ?> equipments) {
            Map<String, Object> appEquipment = new LinkedHashMap<>();
            for (var entry : ((Map<String, Object>) equipments).entrySet()) {
                Object normalized = unwrapValue(entry.getValue(), "state");
                if (normalized != null) {
                    appEquipment.put(entry.getKey(), normalized);
                }
            }
            if (!appEquipment.isEmpty()) {
                appData.put("equipment", appEquipment);
            }
        }

        Object modeObj = payload.get("mode");
        if (modeObj != null) {
            appData.put("mode", modeObj);
        }

        Object protocolVersion = hwPayload.get("protocol_version");
        if (protocolVersion != null) {
            appData.put("protocol_version", protocolVersion);
        }

        Object traceId = hwPayload.get("trace_id");
        if (traceId != null) {
            appData.put("trace_id", traceId);
        }

        result.put("data", appData);
        return result;
    }

    /**
     * Extract the environment map from an already-translated App-format telemetry for InfluxDB write.
     * Returns flat key-value pairs: { temperature: 25.0, humidity: 60.0, ... }
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> extractEnvironment(Map<String, Object> appPayload) {
        Object dataObj = appPayload.get("data");
        if (dataObj instanceof Map<?, ?> data) {
            Object envObj = data.get("environment");
            if (envObj instanceof Map<?, ?>) {
                return new LinkedHashMap<>((Map<String, Object>) envObj);
            }
        }
        return Map.of();
    }

    /**
     * Extract the equipment map from an already-translated App-format telemetry for InfluxDB write.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> extractEquipment(Map<String, Object> appPayload) {
        Object dataObj = appPayload.get("data");
        if (dataObj instanceof Map<?, ?> data) {
            Object eqObj = data.get("equipment");
            if (eqObj instanceof Map<?, ?>) {
                return new LinkedHashMap<>((Map<String, Object>) eqObj);
            }
        }
        return Map.of();
    }

    private static Object unwrapValue(Object valueOrMap, String field) {
        if (valueOrMap instanceof Map<?, ?> map) {
            Object nested = map.get(field);
            return nested != null ? nested : valueOrMap;
        }
        return valueOrMap;
    }
}
