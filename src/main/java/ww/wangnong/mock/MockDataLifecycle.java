package ww.wangnong.mock;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import ww.wangnong.entity.EquipmentType;
import ww.wangnong.entity.Node;
import ww.wangnong.entity.NodeEquipment;
import ww.wangnong.entity.NodeSensor;
import ww.wangnong.entity.SensorType;
import ww.wangnong.entity.User;
import ww.wangnong.entity.UserNodeAccess;
import ww.wangnong.influx.InfluxTelemetryService;
import ww.wangnong.repository.AlertRepository;
import ww.wangnong.repository.EquipmentTypeRepository;
import ww.wangnong.repository.NodeEquipmentRepository;
import ww.wangnong.repository.NodeRepository;
import ww.wangnong.repository.NodeSensorRepository;
import ww.wangnong.repository.SensorTypeRepository;
import ww.wangnong.repository.UserNodeAccessRepository;
import ww.wangnong.repository.UserRepository;
import ww.wangnong.service.AlertService;

@Component
@ConditionalOnProperty(name = "hmht.run.mode", havingValue = "mock", matchIfMissing = true)
public class MockDataLifecycle implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataLifecycle.class);

    private final UserRepository userRepo;
    private final NodeRepository nodeRepo;
    private final SensorTypeRepository sensorTypeRepo;
    private final NodeSensorRepository nodeSensorRepo;
    private final EquipmentTypeRepository equipmentTypeRepo;
    private final NodeEquipmentRepository nodeEquipmentRepo;
    private final UserNodeAccessRepository userNodeAccessRepo;
    private final AlertService alertService;
    private final AlertRepository alertRepo;
    private final JdbcTemplate jdbc;
    @jakarta.annotation.Nullable
    private final InfluxTelemetryService influxService;

    public MockDataLifecycle(UserRepository userRepo, NodeRepository nodeRepo,
            SensorTypeRepository sensorTypeRepo,
            NodeSensorRepository nodeSensorRepo,
            EquipmentTypeRepository equipmentTypeRepo,
            NodeEquipmentRepository nodeEquipmentRepo,
            UserNodeAccessRepository userNodeAccessRepo,
            AlertService alertService,
            AlertRepository alertRepo,
            JdbcTemplate jdbc,
            @Nullable InfluxTelemetryService influxService) {
        this.userRepo = userRepo;
        this.nodeRepo = nodeRepo;
        this.sensorTypeRepo = sensorTypeRepo;
        this.nodeSensorRepo = nodeSensorRepo;
        this.equipmentTypeRepo = equipmentTypeRepo;
        this.nodeEquipmentRepo = nodeEquipmentRepo;
        this.userNodeAccessRepo = userNodeAccessRepo;
        this.alertService = alertService;
        this.alertRepo = alertRepo;
        this.jdbc = jdbc;
        this.influxService = influxService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepo.count() > 0) {
            log.info("[MockData] 数据已存在，重建种子告警...");
            // 删除旧种子告警并重建，确保每次启动都有多样化告警
            for (int i = 1; i <= 5; i++) {
                alertRepo.findByAlertId("SEED_" + i).ifPresent(alertRepo::delete);
            }
            seedAlerts();
            log.info("[MockData] 种子告警已重建。");
            return;
        }
        seed();
        log.info("[MockData] 模拟数据初始化完成。");
    }

    @PreDestroy
    public void cleanup() {
        log.info("[MockData] 正在清除数据库数据...");
        String[] tables = { "command_retry_log", "device_shadow", "capability_equipment",
                "capability_sensor", "device_capability", "device_profile",
                "command_ack", "command_request", "ingest_deadletter", "ingest_audit",
                "scheduled_tasks", "operation_logs", "user_node_access",
                "chat_messages", "command_logs", "alerts", "node_sensor",
                "node_equipment", "sensor_type", "equipment_type",
                "verification_codes", "nodes", "users" };
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : tables) {
            try {
                jdbc.execute("TRUNCATE TABLE " + table);
            } catch (Exception e) {
                log.warn("[MockData] 清除表 {} 失败: {}", table, e.getMessage());
            }
        }
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
        log.info("[MockData] 数据库数据已清除。");
    }

    private void seed() {
        // 1. 用户（普通用户 + 管理员）
        User user = new User();
        user.setPhone("13800000000");
        user.setPasswordHash("123456");
        user.setUserName("农场主");
        user.setGender("男");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setAvatarSrc("https://example.com/avatar.jpg");
        user.setRole("user");
        user = userRepo.save(user);

        User admin = new User();
        admin.setPhone("13800000001");
        admin.setPasswordHash("admin123");
        admin.setUserName("管理员");
        admin.setGender("男");
        admin.setBirthDate(LocalDate.of(1985, 6, 15));
        admin.setRole("admin");
        admin = userRepo.save(admin);

        // 2. 节点
        seedNode(user, "A03_Node_01", "温室节点 1");
        seedNode(user, "A03_Node_02", "温室节点 2");

        // 3. 全局传感器类型 (6 种)
        seedSensorTypes();

        // 4. 全局设备类型 (4 种)
        seedEquipmentTypes();

        // 5. 每节点传感器关联 + 阈值
        seedNodeSensors("A03_Node_01", true);
        seedNodeSensors("A03_Node_02", false);

        // 6. 每节点设备关联
        seedNodeEquipment("A03_Node_01");
        seedNodeEquipment("A03_Node_02");

        // 7. 用户-节点访问授权 (admin 自动拥有全部，不需要额外记录)
        UserNodeAccess access = new UserNodeAccess();
        access.setUserId(user.getId());
        access.setNodeId("A03_Node_01");
        userNodeAccessRepo.save(access);
        UserNodeAccess access2 = new UserNodeAccess();
        access2.setUserId(user.getId());
        access2.setNodeId("A03_Node_02");
        userNodeAccessRepo.save(access2);

        // 8. InfluxDB 30天历史模拟数据
        seedInfluxData();

        // 9. 初始告警数据
        seedAlerts();
    }

    private void seedInfluxData() {
        if (influxService == null) {
            log.info("[MockData] InfluxDB 未启用，跳过历史数据生成。");
            return;
        }
        log.info("[MockData] 开始生成 30 天 InfluxDB 模拟数据...");
        Random rng = new Random(42);
        long now = System.currentTimeMillis();
        long thirtyDaysMs = 30L * 24 * 60 * 60 * 1000;
        long intervalMs = 10 * 60 * 1000; // 10 minutes

        String[] nodes = { "A03_Node_01", "A03_Node_02" };
        for (String nodeId : nodes) {
            double temp = 22.0, hum = 55.0, co2 = 420.0;
            double light = 8000.0, soilT = 18.0, soilH = 35.0;

            for (long ts = now - thirtyDaysMs; ts <= now; ts += intervalMs) {
                temp = drift(rng, temp, 0.5, 10, 42);
                hum = drift(rng, hum, 1.5, 20, 98);
                co2 = drift(rng, co2, 8, 200, 2000);
                light = drift(rng, light, 300, 0, 50000);
                soilT = drift(rng, soilT, 0.3, 3, 38);
                soilH = drift(rng, soilH, 0.8, 10, 85);

                Map<String, Double> env = Map.of(
                        "temperature", round1(temp), "humidity", round1(hum),
                        "co2", round1(co2), "light", round1(light),
                        "soilTemp", round1(soilT), "soilHumidity", round1(soilH));
                influxService.writeEnvironment(nodeId, ts, env);

                Map<String, Boolean> eq = Map.of(
                        "water_valve", rng.nextDouble() < 0.1,
                        "water_pump", rng.nextDouble() < 0.15,
                        "led_light", light < 5000,
                        "camera", true);
                influxService.writeEquipmentState(nodeId, ts, eq, "manual");
            }
        }
        log.info("[MockData] InfluxDB 30 天模拟数据生成完成。");
    }

    private static double drift(Random rng, double val, double step, double min, double max) {
        return Math.max(min, Math.min(max, val + (rng.nextDouble() * 2 - 1) * step));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private void seedNode(User user, String nodeId, String name) {
        Node n = new Node();
        n.setNodeId(nodeId);
        n.setNodeName(name);
        n.setUser(user);
        n.setOnline(true);
        n.setSmartMode(false);
        nodeRepo.save(n);
    }

    private void seedSensorTypes() {
        record ST(String key, String name, String unit, String icon, String badgeColor, String bgColor) {
        }

        List<ST> types = List.of(
                new ST("temperature", "空气温度", "℃", "🌡️", "#FF7043", "#FFF3E0"),
                new ST("humidity", "空气湿度", "%", "💧", "#42A5F5", "#E3F2FD"),
                new ST("co2", "CO₂浓度", "ppm", "🫧", "#78909C", "#ECEFF1"),
                new ST("light", "光照强度", "Lux", "☀️", "#FDD835", "#FFFDE7"),
                new ST("soilTemp", "土壤温度", "℃", "🌱", "#8D6E63", "#EFEBE9"),
                new ST("soilHumidity", "土壤湿度", "%", "🪴", "#66BB6A", "#E8F5E9"));

        for (ST s : types) {
            SensorType t = new SensorType();
            t.setSensorKey(s.key());
            t.setName(s.name());
            t.setUnit(s.unit());
            t.setIcon(s.icon());
            t.setBadgeColor(s.badgeColor());
            t.setBgColor(s.bgColor());
            sensorTypeRepo.save(t);
        }
    }

    private void seedEquipmentTypes() {
        record ET(String key, String name, String icon, boolean ctrl) {
        }

        List<ET> types = List.of(
                new ET("water_valve", "水阀", "🚿", true),
                new ET("water_pump", "水泵", "💦", true),
                new ET("led_light", "补光灯", "💡", true),
                new ET("camera", "摄像头", "📷", false));

        for (ET e : types) {
            EquipmentType t = new EquipmentType();
            t.setEquipmentKey(e.key());
            t.setName(e.name());
            t.setIcon(e.icon());
            t.setControllable(e.ctrl());
            equipmentTypeRepo.save(t);
        }
    }

    private void seedNodeSensors(String nodeId, boolean fullThresholds) {
        record NS(String key, Double warnMin, Double warnMax) {
        }

        List<NS> sensors = List.of(
                new NS("temperature", 10.0, 35.0),
                new NS("humidity", 30.0, 85.0),
                new NS("co2", null, 1500.0),
                new NS("light", null, null),
                new NS("soilTemp", fullThresholds ? 5.0 : null, fullThresholds ? 35.0 : null),
                new NS("soilHumidity", fullThresholds ? 20.0 : null, fullThresholds ? 80.0 : null));

        for (NS s : sensors) {
            NodeSensor ns = new NodeSensor();
            ns.setNodeId(nodeId);
            ns.setSensorKey(s.key());
            ns.setWarningMin(s.warnMin());
            ns.setWarningMax(s.warnMax());
            nodeSensorRepo.save(ns);
        }
    }

    private void seedNodeEquipment(String nodeId) {
        for (String key : List.of("water_valve", "water_pump", "led_light", "camera")) {
            NodeEquipment ne = new NodeEquipment();
            ne.setNodeId(nodeId);
            ne.setEquipmentKey(key);
            nodeEquipmentRepo.save(ne);
        }
    }

    private void seedAlerts() {
        long now = System.currentTimeMillis();
        record A(String nodeId, String type, String typeName, String level,
                String message, String source, String sourceName, boolean handled, long offsetMs) {
        }

        List<A> alerts = List.of(
                new A("A03_Node_01", "high_temp", "高温告警", "warning",
                        "空气温度 38.2℃ 超过阈值 35.0℃", "temperature", "空气温度", false, 10 * 60_000L),
                new A("A03_Node_01", "high_co2", "CO₂超标", "warning",
                        "CO₂浓度 1620ppm 超过阈值 1500ppm", "co2", "CO₂浓度", false, 30 * 60_000L),
                new A("A03_Node_02", "low_humidity", "低湿度告警", "info",
                        "空气湿度 25.3% 低于阈值 30.0%", "humidity", "空气湿度", false, 2 * 3600_000L),
                new A("A03_Node_01", "high_temp", "高温告警", "danger",
                        "空气温度 41.5℃ 超过阈值 35.0℃", "temperature", "空气温度", true, 8 * 3600_000L),
                new A("A03_Node_01", "low_soil_humidity", "土壤湿度过低", "warning",
                        "土壤湿度 15.2% 低于阈值 20.0%", "soilHumidity", "土壤湿度", true, 24 * 3600_000L));

        for (int i = 0; i < alerts.size(); i++) {
            A a = alerts.get(i);
            long ts = now - a.offsetMs();
            alertService.addAlert(new AlertService.AlertInfo(
                    "SEED_" + (i + 1), a.nodeId(), a.type(), a.typeName(),
                    a.level(), a.message(), a.source(), a.sourceName(),
                    a.handled(), a.handled() ? ts + 60_000L : null, ts));
        }
        log.info("[MockData] 已生成 {} 条初始告警。", alerts.size());
    }
}
