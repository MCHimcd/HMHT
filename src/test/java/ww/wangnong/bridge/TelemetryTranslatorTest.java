package ww.wangnong.bridge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TelemetryTranslatorTest {

    @Test
    void pyToApp_translatesEnvironmentKeys() {
        Map<String, Object> pyPayload = new LinkedHashMap<>();
        pyPayload.put("msg_type", "telemetry");
        pyPayload.put("device_id", "D01");
        pyPayload.put("timestamp", 1700000000000L);

        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("air_temp", 25.0);
        env.put("air_humidity", 60.0);
        env.put("soil_temp", 20.0);
        env.put("soil_moisture", 40.0);
        env.put("soil_ph", 6.5);
        env.put("light_intensity", 10000.0);
        env.put("co2_ppm", 400.0);
        env.put("soil_n", 30.0);
        env.put("soil_p", 15.0);
        env.put("soil_k", 20.0);
        data.put("environment", env);
        pyPayload.put("data", data);

        Map<String, Object> result = TelemetryTranslator.pyToApp(pyPayload);

        assertThat(result.get("msg_type")).isEqualTo("telemetry");
        assertThat(result.get("node_id")).isEqualTo("D01");
        assertThat(result.get("timestamp")).isEqualTo(1700000000000L);

        @SuppressWarnings("unchecked")
        Map<String, Object> appData = (Map<String, Object>) result.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> appEnv = (Map<String, Object>) appData.get("environment");

        assertThat(appEnv.get("temperature")).isEqualTo(25.0);
        assertThat(appEnv.get("humidity")).isEqualTo(60.0);
        assertThat(appEnv.get("soilTemp")).isEqualTo(20.0);
        assertThat(appEnv.get("soilHumidity")).isEqualTo(40.0);
        assertThat(appEnv.get("soilPh")).isEqualTo(6.5);
        assertThat(appEnv.get("light")).isEqualTo(10000.0);
        assertThat(appEnv.get("co2")).isEqualTo(400.0);
        assertThat(appEnv.get("soilN")).isEqualTo(30.0);
        assertThat(appEnv.get("soilP")).isEqualTo(15.0);
        assertThat(appEnv.get("soilK")).isEqualTo(20.0);
    }

    @Test
    void pyToApp_translatesEquipmentStatusToEquipment() {
        Map<String, Object> pyPayload = new LinkedHashMap<>();
        pyPayload.put("msg_type", "telemetry");
        pyPayload.put("device_id", "D01");
        pyPayload.put("timestamp", 1700000000000L);

        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> eqStatus = new LinkedHashMap<>();
        eqStatus.put("water_pump", true);
        eqStatus.put("fan", false);
        data.put("equipment_status", eqStatus);
        data.put("system_mode", "auto");
        pyPayload.put("data", data);

        Map<String, Object> result = TelemetryTranslator.pyToApp(pyPayload);

        @SuppressWarnings("unchecked")
        Map<String, Object> appData = (Map<String, Object>) result.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> equipment = (Map<String, Object>) appData.get("equipment");

        assertThat(equipment.get("water_pump")).isEqualTo(true);
        assertThat(equipment.get("fan")).isEqualTo(false);
        assertThat(appData.get("mode")).isEqualTo("auto");
    }

    @Test
    void pyToApp_nonMapData_preservesAsIs() {
        Map<String, Object> pyPayload = new LinkedHashMap<>();
        pyPayload.put("msg_type", "telemetry");
        pyPayload.put("device_id", "D01");
        pyPayload.put("timestamp", 1700000000000L);
        pyPayload.put("data", "raw-string");

        Map<String, Object> result = TelemetryTranslator.pyToApp(pyPayload);

        assertThat(result.get("data")).isEqualTo("raw-string");
    }

    @Test
    void extractEnvironment_returnsEnvironmentMap() {
        Map<String, Object> appPayload = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("temperature", 25.0);
        env.put("humidity", 60.0);
        data.put("environment", env);
        appPayload.put("data", data);

        Map<String, Object> result = TelemetryTranslator.extractEnvironment(appPayload);

        assertThat(result).hasSize(2);
        assertThat(result.get("temperature")).isEqualTo(25.0);
        assertThat(result.get("humidity")).isEqualTo(60.0);
    }

    @Test
    void extractEnvironment_noData_returnsEmpty() {
        Map<String, Object> appPayload = new LinkedHashMap<>();

        Map<String, Object> result = TelemetryTranslator.extractEnvironment(appPayload);

        assertThat(result).isEmpty();
    }

    @Test
    void extractEquipment_returnsEquipmentMap() {
        Map<String, Object> appPayload = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> eq = new LinkedHashMap<>();
        eq.put("water_pump", true);
        data.put("equipment", eq);
        appPayload.put("data", data);

        Map<String, Object> result = TelemetryTranslator.extractEquipment(appPayload);

        assertThat(result).hasSize(1);
        assertThat(result.get("water_pump")).isEqualTo(true);
    }

    @Test
    void extractEquipment_noData_returnsEmpty() {
        Map<String, Object> appPayload = new LinkedHashMap<>();

        Map<String, Object> result = TelemetryTranslator.extractEquipment(appPayload);

        assertThat(result).isEmpty();
    }
}
