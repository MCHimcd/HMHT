package ww.wangnong.bridge;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * SensorService backed by InfluxDB — reads real telemetry history written by
 * the bridge.
 * Active only when bridge mode is on AND InfluxDB client is available.
 */
@Service
@Primary
@ConditionalOnProperty(prefix = "hmht.influxdb", name = "enabled", havingValue = "true")
@ConditionalOnBean(InfluxDBClient.class)
public class InfluxSensorService implements SensorService {

    private static final Logger log = LoggerFactory.getLogger(InfluxSensorService.class);

    private final InfluxDBClient influxClient;

    public InfluxSensorService(InfluxDBClient influxClient) {
        this.influxClient = influxClient;
    }

    @Override
    public SensorHistoryResult getSensorHistory(String nodeId, String metric, String range) {
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
            return modernResult.get();
        }

        String legacySql = String.format(
            "SELECT date_bin('%s', time) AS t, AVG(\"%s\") AS val "
                + "FROM environment WHERE node = '%s' AND %s "
                + "GROUP BY 1 ORDER BY 1",
            interval, sanitizeMetricField(metric), sanitizeTag(nodeId), rangeFilter);

        return queryHistory(metric, fmt, legacySql)
            .orElseGet(() -> new SensorHistoryResult(metric, new HistorySummary(0, 0, 0), List.of()));
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
}
