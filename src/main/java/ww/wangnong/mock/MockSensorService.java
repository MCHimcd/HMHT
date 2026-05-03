package ww.wangnong.mock;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import ww.wangnong.service.AlertService;
import ww.wangnong.service.SensorService;

/**
 * Purpose: 在 mock 模式下生成传感器历史和设备统计的演示数据。
 * Responsibilities: 基于指标基准值生成平滑随机曲线，并返回最近 7 天设备统计。
 * Flow: 控制器查询 -> 按 metric/range 计算采样点 -> 生成 summary 与 points -> 返回结果。
 * Dependencies: SensorService。
 * Input: nodeId、metric、range、equipmentKey。
 * Output: 可视化友好的历史曲线与统计数据。
 */
@Service
public class MockSensorService implements SensorService {

    private static final Random RNG = new Random();
    private static final double LOW_TEMPERATURE_THRESHOLD = 5.0;
    private static final long LOW_TEMPERATURE_ALERT_COOLDOWN_MS = 60_000L;
    private static final int HISTORY_24H_POINTS = 48;
    private static final int HISTORY_7D_POINTS = 42;
    private static final int HISTORY_30D_POINTS = 30;
    private static final long HISTORY_24H_INTERVAL_MS = 1_800_000L;
    private static final long HISTORY_7D_INTERVAL_MS = 4 * 3_600_000L;
    private static final long HISTORY_30D_INTERVAL_MS = 86_400_000L;

    private static final Map<String, double[]> BASE_VALUES = Map.ofEntries(
            Map.entry("temperature", new double[] { 25.0, 3.0 }),
            Map.entry("humidity", new double[] { 60.0, 6.0 }),
            Map.entry("light", new double[] { 900.0, 300.0 }),
            Map.entry("co2", new double[] { 425.0, 15.0 }),
            Map.entry("soilTemp", new double[] { 20.0, 3.0 }),
            Map.entry("soilHumidity", new double[] { 70.0, 6.0 }),
            Map.entry("soilPh", new double[] { 6.8, 0.3 }),
            Map.entry("soilN", new double[] { 165.0, 40.0 }),
            Map.entry("soilP", new double[] { 165.0, 40.0 }),
            Map.entry("soilK", new double[] { 165.0, 40.0 }));

    private final AlertService alertService;
    private final AtomicInteger temperatureRequestCount = new AtomicInteger();
    private final ConcurrentMap<String, Long> lowTemperatureAlertTimes = new ConcurrentHashMap<>();
    private final ConcurrentMap<HistoryKey, HistoryWindow> historyCache = new ConcurrentHashMap<>();

    public MockSensorService(AlertService alertService) {
        this.alertService = alertService;
    }

    @Override
    public SensorHistoryResult getSensorHistory(String nodeId, String metric, String range) {
        double[] bv = BASE_VALUES.getOrDefault(metric, new double[] { 25.0, 3.0 });
        double base = bv[0], variance = bv[1];
        int requestNo = isLiveTemperature(metric, range) ? temperatureRequestCount.incrementAndGet() : 0;
        long now = System.currentTimeMillis();
        HistorySpec spec = HistorySpec.forRange(range);
        HistoryKey key = new HistoryKey(nodeId, metric, range);
        HistoryWindow window = historyCache.compute(key, (ignored, existing) -> updateWindow(
                existing, base, variance, spec, now, shouldSimulateLowTemperature(requestNo)));
        List<HistoryPoint> points = window.points();
        maybeCreateLowTemperatureAlert(nodeId, metric, range, points.getLast().value(), now);
        return new SensorHistoryResult(metric, summarize(points), points);
    }

    @Override
    public EquipmentStatsResult getEquipmentStats(String nodeId, String equipmentKey) {
        Map<String, String> units = Map.ofEntries(
                Map.entry("water_valve", "开阀次数"),
                Map.entry("water_pump", "灌溶量(升)"),
                Map.entry("led_light", "补光时长(H)"),
                Map.entry("fan", "运行时长(H)"),
                Map.entry("camera", "识别次数"),
                Map.entry("fertigation_machine", "运行时长(H)"),
                Map.entry("air_source_ac", "运行时长(H)"),
                Map.entry("led_display", "显示时长(H)"),
                Map.entry("electric_skylight", "开窗次数"));
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

    private static boolean isLiveTemperature(String metric, String range) {
        return "temperature".equals(metric) && "24h".equals(range);
    }

    private static boolean shouldSimulateLowTemperature(int requestNo) {
        return requestNo > 0 && requestNo % 4 == 0;
    }

    private static HistoryWindow updateWindow(HistoryWindow existing, double base, double variance,
            HistorySpec spec, long now, boolean lowTemperaturePoint) {
        if (existing == null) {
            return initializeWindow(base, variance, spec, now, lowTemperaturePoint);
        }

        List<HistoryPoint> points = new ArrayList<>(existing.points().size());
        if (existing.points().size() > 1) {
            points.addAll(existing.points().subList(1, existing.points().size()));
        }
        long ts = Math.max(now, existing.lastTimestamp() + 1);
        points.add(new HistoryPoint(formatTime(ts, spec.formatter()), nextValue(base, variance, lowTemperaturePoint)));
        return new HistoryWindow(List.copyOf(points), ts);
    }

    private static HistoryWindow initializeWindow(double base, double variance,
            HistorySpec spec, long now, boolean lowTemperaturePoint) {
        List<HistoryPoint> points = new ArrayList<>(spec.size());
        long firstTs = now - (long) (spec.size() - 1) * spec.intervalMs();
        for (int i = 0; i < spec.size(); i++) {
            long ts = firstTs + (long) i * spec.intervalMs();
            boolean isLatest = i == spec.size() - 1;
            points.add(new HistoryPoint(formatTime(ts, spec.formatter()),
                    nextValue(base, variance, isLatest && lowTemperaturePoint)));
        }
        return new HistoryWindow(List.copyOf(points), now);
    }

    private static double nextValue(double base, double variance, boolean lowTemperaturePoint) {
        if (lowTemperaturePoint) {
            return Math.round((3.6 + RNG.nextDouble() * 0.9) * 10.0) / 10.0;
        }
        return Math.round((base + (RNG.nextDouble() * 2 - 1) * variance) * 10.0) / 10.0;
    }

    private static HistorySummary summarize(List<HistoryPoint> points) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, sum = 0;
        for (HistoryPoint point : points) {
            min = Math.min(min, point.value());
            max = Math.max(max, point.value());
            sum += point.value();
        }
        double avg = Math.round(sum / points.size() * 10.0) / 10.0;
        return new HistorySummary(max, min, avg);
    }

    private static String formatTime(long ts, DateTimeFormatter formatter) {
        return Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).format(formatter);
    }

    private void maybeCreateLowTemperatureAlert(String nodeId, String metric, String range, double value, long now) {
        if (!isLiveTemperature(metric, range) || value >= LOW_TEMPERATURE_THRESHOLD) {
            return;
        }

        Long previous = lowTemperatureAlertTimes.get(nodeId);
        if (previous != null && now - previous < LOW_TEMPERATURE_ALERT_COOLDOWN_MS) {
            return;
        }
        lowTemperatureAlertTimes.put(nodeId, now);
        String alertId = "COURSE_LOW_TEMP_" + nodeId + "_" + now / LOW_TEMPERATURE_ALERT_COOLDOWN_MS;
        alertService.addAlert(new AlertService.AlertInfo(
                alertId,
                nodeId,
                "temperature_low",
                "空气温度",
                "warning",
                "模拟温室空气温度低于 5C，请检查保温设备。",
                "sensor",
                "空气温度传感器",
                false,
                null,
                now));
    }

    private record HistoryKey(String nodeId, String metric, String range) {
    }

    private record HistoryWindow(List<HistoryPoint> points, long lastTimestamp) {
    }

    private record HistorySpec(int size, long intervalMs, DateTimeFormatter formatter) {
        static HistorySpec forRange(String range) {
            return switch (range) {
                case "7d" -> new HistorySpec(HISTORY_7D_POINTS, HISTORY_7D_INTERVAL_MS,
                        DateTimeFormatter.ofPattern("MM-dd HH:00"));
                case "30d" -> new HistorySpec(HISTORY_30D_POINTS, HISTORY_30D_INTERVAL_MS,
                        DateTimeFormatter.ofPattern("MM-dd"));
                default -> new HistorySpec(HISTORY_24H_POINTS, HISTORY_24H_INTERVAL_MS,
                        DateTimeFormatter.ofPattern("HH:mm:ss"));
            };
        }
    }
}
