package ww.wangnong.mock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import ww.wangnong.config.condition.ConditionalOnMockMode;
import ww.wangnong.service.SensorService;

@Service
@ConditionalOnMockMode
public class MockSensorService implements SensorService {

    private static final Random RNG = new Random();

    private static final Map<String, double[]> BASE_VALUES = Map.of(
            "temperature", new double[] { 25.0, 3.0 },
            "humidity", new double[] { 60.0, 10.0 },
            "light", new double[] { 10000.0, 2000.0 },
            "co2", new double[] { 400.0, 50.0 },
            "soilTemp", new double[] { 20.0, 2.0 },
            "soilHumidity", new double[] { 40.0, 5.0 });

    @Override
    public SensorHistoryResult getSensorHistory(String nodeId, String metric, String range) {
        double[] bv = BASE_VALUES.getOrDefault(metric, new double[] { 25.0, 3.0 });
        double base = bv[0], variance = bv[1];

        int numPoints;
        long intervalMs;
        switch (range) {
            case "7d" -> {
                numPoints = 42;
                intervalMs = 4 * 3_600_000L;
            }
            case "30d" -> {
                numPoints = 30;
                intervalMs = 86_400_000L;
            }
            default -> {
                numPoints = 48;
                intervalMs = 1_800_000L;
            }
        }

        long now = System.currentTimeMillis();
        List<HistoryPoint> points = new ArrayList<>(numPoints);
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, sum = 0;

        java.time.format.DateTimeFormatter fmt = switch (range) {
            case "7d" -> java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:00");
            case "30d" -> java.time.format.DateTimeFormatter.ofPattern("MM-dd");
            default -> java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        };

        for (int i = numPoints - 1; i >= 0; i--) {
            long ts = now - (long) i * intervalMs;
            String label = java.time.Instant.ofEpochMilli(ts)
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(fmt);
            double val = Math.round((base + (RNG.nextDouble() * 2 - 1) * variance) * 10.0) / 10.0;
            points.add(new HistoryPoint(label, val));
            min = Math.min(min, val);
            max = Math.max(max, val);
            sum += val;
        }

        double avg = Math.round(sum / numPoints * 10.0) / 10.0;
        return new SensorHistoryResult(metric, new HistorySummary(max, min, avg), points);
    }

    @Override
    public EquipmentStatsResult getEquipmentStats(String nodeId, String equipmentKey) {
        Map<String, String> units = Map.of(
                "water_valve", "开阀次数",
                "water_pump", "灌溉量(升)",
                "led_light", "补光时长(H)",
                "camera", "识别次数");
        String unit = units.getOrDefault(equipmentKey, "值");

        List<StatsPoint> points = new ArrayList<>();
        double total = 0;
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            double val = Math.round(RNG.nextDouble() * 100 * 10.0) / 10.0;
            points.add(new StatsPoint(d.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd")), val));
            total += val;
        }

        return new EquipmentStatsResult(
                Map.of("total_value", Math.round(total * 10.0) / 10.0, "unit", unit),
                points);
    }
}
