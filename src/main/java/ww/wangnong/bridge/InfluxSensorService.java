package ww.wangnong.bridge;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.influxdb.v3.client.InfluxDBClient;
import com.influxdb.v3.client.query.QueryOptions;
import com.influxdb.v3.client.query.QueryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import ww.wangnong.service.SensorService;

/**
 * Purpose: 通过 InfluxDB 提供真实遥测历史与设备统计查询。
 * Responsibilities: 查询新旧 measurement、处理超时与失败熔断，并在无数据或失败时生成平滑回退数据。
 * Flow: getSensorHistory -> 熔断检查 -> Influx SQL 查询 -> 若失败则打开熔断并返回
 * fallback；设备统计直接查询 equipment_state。
 * Dependencies: InfluxDBClient、SensorService、hmht.influxdb.enabled 配置。
 * Input: nodeId、metric、range、equipmentKey。
 * Output: 传感器历史结果和设备统计结果。
 * Boundary: Influx 查询超时后会短暂打开熔断，避免请求线程持续阻塞在外部时序库上。
 */
@Service
@Primary
@ConditionalOnProperty(prefix = "hmht.influxdb", name = "enabled", havingValue = "true")
@ConditionalOnBean(InfluxDBClient.class)
public class InfluxSensorService implements SensorService {

    private static final Logger log = LoggerFactory.getLogger(InfluxSensorService.class);
    private static final Random RNG = new Random();

    private static final long CIRCUIT_OPEN_MS = 60_000;
    private static final long QUERY_TIMEOUT_MS = 1_500;

    private volatile boolean circuitOpen = false;
    private volatile long circuitRetryAt = 0;

    private static final Map<String, double[]> FALLBACK_BASE_VALUES = Map.ofEntries(
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

    private final InfluxDBClient influxClient;

    public InfluxSensorService(InfluxDBClient influxClient) {
        this.influxClient = influxClient;
    }

    @Override
    public SensorHistoryResult getSensorHistory(String nodeId, String metric, String range) {
        // Circuit breaker: skip InfluxDB queries if recently failed
        if (circuitOpen && System.currentTimeMillis() < circuitRetryAt) {
            log.debug("[InfluxSensor] Circuit open, returning fallback for {}/{}", nodeId, metric);
            return generateFallbackHistory(metric, range);
        }

        // Wrap InfluxDB queries in a future with timeout to prevent blocking
        try {
            return CompletableFuture.supplyAsync(() -> queryInfluxHistory(nodeId, metric, range))
                    .get(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            openCircuit();
            log.warn("[InfluxSensor] Query timed out or failed for node={}, metric={}, range={}; using fallback",
                    nodeId, metric, range);
            return generateFallbackHistory(metric, range);
        }
    }

    private SensorHistoryResult queryInfluxHistory(String nodeId, String metric, String range) {
        String interval;
        String rangeFilter;
        DateTimeFormatter fmt;

        switch (range) {
            case "7d" -> {
                rangeFilter = "time >= now() - 7d";
                interval = "1 hour";
                fmt = DateTimeFormatter.ofPattern("MM-dd HH:00");
            }
            case "30d" -> {
                rangeFilter = "time >= now() - 30d";
                interval = "1 day";
                fmt = DateTimeFormatter.ofPattern("MM-dd");
            }
            default -> { // 24h
                rangeFilter = "time >= now() - 1d";
                interval = "30 minutes";
                fmt = DateTimeFormatter.ofPattern("HH:mm");
            }
        }

        String newSql = String.format(
                "SELECT date_bin('%s', time) AS t, AVG(value) AS val "
                        + "FROM telemetry_metric WHERE node = '%s' AND metric = '%s' AND %s "
                        + "GROUP BY 1 ORDER BY 1",
                interval, sanitizeTag(nodeId), sanitizeMetricTag(metric), rangeFilter);

        Optional<SensorHistoryResult> modernResult = queryHistory(metric, fmt, newSql);
        if (modernResult.isPresent() && !modernResult.get().points().isEmpty()) {
            closeCircuit();
            return modernResult.get();
        }

        String legacySql = String.format(
                "SELECT date_bin('%s', time) AS t, AVG(\"%s\") AS val "
                        + "FROM environment WHERE node = '%s' AND %s "
                        + "GROUP BY 1 ORDER BY 1",
                interval, sanitizeMetricField(metric), sanitizeTag(nodeId), rangeFilter);

        return queryHistory(metric, fmt, legacySql)
                .filter(r -> !r.points().isEmpty())
                .map(r -> {
                    closeCircuit();
                    return r;
                })
                .orElseGet(() -> {
                    openCircuit();
                    log.info(
                            "[InfluxSensor] No InfluxDB data for node={}, metric={}, range={}; returning fallback data",
                            nodeId, metric, range);
                    return generateFallbackHistory(metric, range);
                });
    }

    private void openCircuit() {
        circuitOpen = true;
        circuitRetryAt = System.currentTimeMillis() + CIRCUIT_OPEN_MS;
    }

    private void closeCircuit() {
        circuitOpen = false;
    }

    @Override
    public EquipmentStatsResult getEquipmentStats(String nodeId, String equipmentKey) {
        Map<String, String> units = Map.of(
                "water_pump", "灌溉量(升)",
                "led_light", "补光时长(H)",
                "ai_camera", "识别病虫(次)",
                "fan", "排风时长(H)",
                "heater", "加热时长(H)",
                "misting", "加湿次数",
                "co2_generator", "补碳时长(H)",
                "fertilizer", "施肥量(ml)");
        String unit = units.getOrDefault(equipmentKey, "值");

        String sql = String.format(
                "SELECT date_bin('1 day', time) AS t, "
                        + "SUM(CASE WHEN status = 'on' THEN 1 ELSE 0 END) AS on_count "
                        + "FROM equipment_state "
                        + "WHERE node = '%s' AND equipment = '%s' AND time >= now() - 7d "
                        + "GROUP BY 1 ORDER BY 1",
                sanitizeTag(nodeId), sanitizeTag(equipmentKey));

        try (@SuppressWarnings("null")
        Stream<Object[]> stream = influxClient.query(sql, new QueryOptions("", QueryType.SQL))) {
            List<StatsPoint> points = new ArrayList<>();
            double total = 0;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

            var iterator = stream.iterator();
            while (iterator.hasNext()) {
                Object[] row = iterator.next();
                if (row.length < 2 || row[1] == null)
                    continue;
                Instant ts = row[0] instanceof Instant inst ? inst : Instant.parse(String.valueOf(row[0]));
                double val = row[1] instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(row[1]));
                val = Math.round(val * 10.0) / 10.0;

                String label = ts.atZone(ZoneId.systemDefault()).format(fmt);
                points.add(new StatsPoint(label, val));
                total += val;
            }

            // Fill gaps with zeros if needed
            if (points.isEmpty()) {
                for (int i = 6; i >= 0; i--) {
                    String label = LocalDate.now().minusDays(i).format(DateTimeFormatter.ofPattern("MM-dd"));
                    points.add(new StatsPoint(label, 0));
                }
            }

            return new EquipmentStatsResult(
                    Map.of("total_value", Math.round(total * 10.0) / 10.0, "unit", unit),
                    points);

        } catch (Exception e) {
            log.warn("[InfluxSensor] Equipment query failed for {}", equipmentKey, e);
            List<StatsPoint> empty = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                String label = LocalDate.now().minusDays(i).format(DateTimeFormatter.ofPattern("MM-dd"));
                empty.add(new StatsPoint(label, 0));
            }
            return new EquipmentStatsResult(Map.of("total_value", 0.0, "unit", unit), empty);
        }
    }

    /** Basic sanitization for SQL tag values — prevent injection. */
    private static String sanitizeTag(String value) {
        return value.replaceAll("[^a-zA-Z0-9_\\-.]", "");
    }

    /** Sanitize metric field name for use in SQL column reference. */
    private static String sanitizeMetricField(String metric) {
        return metric.replaceAll("[^a-zA-Z0-9_]", "");
    }

    private static String sanitizeMetricTag(String metric) {
        return metric.replaceAll("[^a-zA-Z0-9_\\-.]", "");
    }

    private Optional<SensorHistoryResult> queryHistory(String metric, DateTimeFormatter fmt, String sql) {
        try (@SuppressWarnings("null")
        Stream<Object[]> stream = influxClient.query(sql, new QueryOptions("", QueryType.SQL))) {
            List<HistoryPoint> points = new ArrayList<>();
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, sum = 0;
            int count = 0;

            var iterator = stream.iterator();
            while (iterator.hasNext()) {
                Object[] row = iterator.next();
                if (row.length < 2 || row[1] == null) {
                    continue;
                }

                Instant ts = row[0] instanceof Instant inst ? inst : Instant.parse(String.valueOf(row[0]));
                double val = row[1] instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(row[1]));
                val = Math.round(val * 10.0) / 10.0;

                String label = ts.atZone(ZoneId.systemDefault()).format(fmt);
                points.add(new HistoryPoint(label, val));
                min = Math.min(min, val);
                max = Math.max(max, val);
                sum += val;
                count++;
            }

            if (count == 0) {
                return Optional.of(new SensorHistoryResult(metric, new HistorySummary(0, 0, 0), List.of()));
            }

            double avg = Math.round(sum / count * 10.0) / 10.0;
            return Optional.of(new SensorHistoryResult(metric, new HistorySummary(max, min, avg), points));
        } catch (Exception e) {
            log.warn("[InfluxSensor] Query failed for metric={}, sql={}", metric, sql, e);
            return Optional.empty();
        }
    }

    /**
     * Generate synthetic history data as a fallback when InfluxDB has no data.
     * Keeps the UI functional for demo nodes and during InfluxDB cold-start.
     */
    SensorHistoryResult generateFallbackHistory(String metric, String range) {
        double[] bv = FALLBACK_BASE_VALUES.getOrDefault(metric, new double[] { 25.0, 3.0 });
        double base = bv[0], variance = bv[1];

        int numPoints;
        long intervalMs;
        DateTimeFormatter fmt;
        switch (range) {
            case "7d" -> {
                numPoints = 42;
                intervalMs = 4 * 3_600_000L;
                fmt = DateTimeFormatter.ofPattern("MM-dd HH:00");
            }
            case "30d" -> {
                numPoints = 30;
                intervalMs = 86_400_000L;
                fmt = DateTimeFormatter.ofPattern("MM-dd");
            }
            default -> {
                numPoints = 48;
                intervalMs = 1_800_000L;
                fmt = DateTimeFormatter.ofPattern("HH:mm");
            }
        }

        long now = System.currentTimeMillis();
        List<HistoryPoint> points = new ArrayList<>(numPoints);
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, sum = 0;

        for (int i = numPoints - 1; i >= 0; i--) {
            long ts = now - (long) i * intervalMs;
            String label = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).format(fmt);
            double val = Math.round((base + (RNG.nextDouble() * 2 - 1) * variance) * 10.0) / 10.0;
            points.add(new HistoryPoint(label, val));
            min = Math.min(min, val);
            max = Math.max(max, val);
            sum += val;
        }

        double avg = Math.round(sum / numPoints * 10.0) / 10.0;
        return new SensorHistoryResult(metric, new HistorySummary(max, min, avg), points);
    }
}
