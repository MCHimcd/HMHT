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
import ww.wangnong.service.OperationLogService;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Purpose: 管理 mock 模式下的种子数据初始化与退出清理。
 * Responsibilities: 初始化演示用户、节点、字典、绑定、Influx 历史、告警与操作日志，并在停机时清空演示数据。
 * Flow: 启动时 run 判断是否已有节点 -> 增量重建或全量 seed；停机时 cleanup 关闭外键后按表顺序 TRUNCATE。
 * Dependencies: 多个
 * Repository、InfluxTelemetryService、JdbcTemplate、PasswordEncoder、AlertService。
 * Input: 当前数据库状态、Influx 启用情况。
 * Output: 完整的 mock 演示数据集。
 * Side Effects: 大量写入数据库与 InfluxDB；停机时会清空 mock 相关表。
 * Boundary: cleanup 依赖手工表顺序和关闭外键检查，不能在非 mock 运行环境误触发。
 */
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
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService opLogService;

    public MockDataLifecycle(UserRepository userRepo, NodeRepository nodeRepo,
            SensorTypeRepository sensorTypeRepo,
            NodeSensorRepository nodeSensorRepo,
            EquipmentTypeRepository equipmentTypeRepo,
            NodeEquipmentRepository nodeEquipmentRepo,
            UserNodeAccessRepository userNodeAccessRepo,
            AlertService alertService,
            AlertRepository alertRepo,
            JdbcTemplate jdbc,
            @Nullable InfluxTelemetryService influxService,
            PasswordEncoder passwordEncoder,
            OperationLogService opLogService) {
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
        this.passwordEncoder = passwordEncoder;
        this.opLogService = opLogService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (nodeRepo.count() > 0) {
            seedSensorTypes();
            seedEquipmentTypes();
            reseedSimNodeBindings();
            log.info("[MockData] 模拟节点已存在，重建种子告警...");
            // 删除旧种子告警并重建，确保每次启动都有多样化告警
            for (int i = 1; i <= 5; i++) {
                alertRepo.findByAlertId("SEED_" + i).ifPresent(alertRepo::delete);
            }
            seedAlerts();
            // 补种操作记录（cleanup 会清空 operation_logs）
            userRepo.findByPhone("13800000000").ifPresent(u -> seedOperationLogs(u.getId(), u.getUserName()));
            log.info("[MockData] 种子告警与操作记录已重建。");
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
                "chat_messages", "alerts", "node_sensor",
                "node_equipment", "sensor_type", "equipment_type",
                "verification_codes", "nodes", "users" };
        // Invariant: cleanup 通过关闭外键并按固定顺序 TRUNCATE，目标是把 mock 环境恢复到“仅剩基础结构”的状态。
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
        // 1. 用户（普通用户 + 管理员）— 使用 findByPhone 避免与 EssentialDataSeeder 冲突
        User user = userRepo.findByPhone("13800000000").orElseGet(() -> {
            User u = new User();
            u.setPhone("13800000000");
            u.setPasswordHash(passwordEncoder.encode("123456"));
            u.setUserName("农场主");
            u.setGender("男");
            u.setBirthDate(LocalDate.of(1990, 1, 1));
            u.setAvatarSrc("https://example.com/avatar.jpg");
            u.setRole("user");
            return userRepo.save(u);
        });

        userRepo.findByPhone("13800000001").orElseGet(() -> {
            User a = new User();
            a.setPhone("13800000001");
            a.setPasswordHash(passwordEncoder.encode("admin123"));
            a.setUserName("管理员");
            a.setGender("男");
            a.setBirthDate(LocalDate.of(1985, 6, 15));
            a.setRole("admin");
            return userRepo.save(a);
        });

        // 2. 节点
        seedNode(user, "A03_Node_01", "温室节点 1");
        seedNode(user, "A03_Node_02", "温室节点 2");
        seedSimNode("SIM_Node_01", "温室节点 3");

        // 3. 全局传感器类型 (13 种：基础 6 + 扩展 7)
        seedSensorTypes();

        // 4. 全局设备类型 (10 种：基础 4 + 扩展 6)
        seedEquipmentTypes();

        // 5. 每节点传感器关联 + 阈值
        seedNodeSensors("A03_Node_01", true);
        seedNodeSensors("A03_Node_02", false);
        seedSimNodeSensors("SIM_Node_01");

        // 6. 每节点设备关联
        seedNodeEquipment("A03_Node_01");
        seedNodeEquipment("A03_Node_02");
        seedSimNodeEquipment("SIM_Node_01");

        // 7. 用户-节点访问授权 (admin 自动拥有全部，不需要额外记录)
        for (String nid : List.of("A03_Node_01", "A03_Node_02", "SIM_Node_01")) {
            UserNodeAccess a = new UserNodeAccess();
            a.setUserId(user.getId());
            a.setNodeId(nid);
            userNodeAccessRepo.save(a);
        }

        // 8. InfluxDB 30天历史模拟数据
        seedInfluxData();

        // 9. 初始告警数据
        seedAlerts();

        // 10. 设备操作记录
        seedOperationLogs(user.getId(), user.getUserName());
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

        // A03 节点：基础 6 传感器 + 4 设备
        for (String nodeId : new String[] { "A03_Node_01", "A03_Node_02" }) {
            double temp = 22.0, hum = 55.0, co2 = 420.0;
            double light = 900.0, soilT = 18.0, soilH = 35.0;

            for (long ts = now - thirtyDaysMs; ts <= now; ts += intervalMs) {
                temp = drift(rng, temp, 0.5, 10, 42);
                hum = drift(rng, hum, 1.5, 20, 98);
                co2 = drift(rng, co2, 8, 200, 2000);
                light = drift(rng, light, 50, 300, 1500);
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
                        "led_light", light < 600,
                        "fan", temp > 30,
                        "camera", true);
                influxService.writeEquipmentState(nodeId, ts, eq, "manual");
            }
        }

        // SIM_Node_01：温室全模拟 10 传感器 + 14 设备
        {
            double temp = 25.0, hum = 60.0, co2 = 425.0, light = 900.0;
            double soilT = 20.0, soilH = 70.0, soilPh = 6.8;
            double soilN = 165.0, soilP = 165.0, soilK = 165.0;

            for (long ts = now - thirtyDaysMs; ts <= now; ts += intervalMs) {
                temp = drift(rng, temp, 0.4, 20, 30);
                hum = drift(rng, hum, 0.8, 50, 70);
                co2 = drift(rng, co2, 2, 400, 450);
                light = drift(rng, light, 50, 300, 1500);
                soilT = drift(rng, soilT, 0.3, 15, 25);
                soilH = drift(rng, soilH, 0.6, 60, 80);
                soilPh = drift(rng, soilPh, 0.03, 6.0, 7.5);
                soilN = drift(rng, soilN, 3, 80, 250);
                soilP = drift(rng, soilP, 3, 80, 250);
                soilK = drift(rng, soilK, 3, 80, 250);

                Map<String, Double> env = new java.util.HashMap<>();
                env.put("temperature", round1(temp));
                env.put("humidity", round1(hum));
                env.put("co2", round1(co2));
                env.put("light", round1(light));
                env.put("soilTemp", round1(soilT));
                env.put("soilHumidity", round1(soilH));
                env.put("soilPh", round1(soilPh));
                env.put("soilN", round1(soilN));
                env.put("soilP", round1(soilP));
                env.put("soilK", round1(soilK));
                influxService.writeEnvironment("SIM_Node_01", ts, env);

                Map<String, Boolean> eq = new java.util.HashMap<>();
                eq.put("water_valve", rng.nextDouble() < 0.12);
                eq.put("water_pump", rng.nextDouble() < 0.15);
                eq.put("led_light", light < 600);
                eq.put("camera", true);
                eq.put("fan", temp > 28);
                eq.put("heater", temp < 18);
                eq.put("shade", light > 1300);
                eq.put("ventilation", co2 > 440);
                eq.put("fertilizer_pump", rng.nextDouble() < 0.05);
                eq.put("mist_sprayer", hum < 55);
                eq.put("fertigation_machine", rng.nextDouble() < 0.08);
                eq.put("air_source_ac", temp > 28 || temp < 18);
                eq.put("led_display", true);
                eq.put("electric_skylight", temp > 27);
                influxService.writeEquipmentState("SIM_Node_01", ts, eq, "manual");
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

    /** 创建无归属用户的纯模拟节点 */
    private void seedSimNode(String nodeId, String name) {
        Node n = new Node();
        n.setNodeId(nodeId);
        n.setNodeName(name);
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
                new ST("light", "光照强度(PAR)", "μmol/m²·s", "☀️", "#FDD835", "#FFFDE7"),
                new ST("soilTemp", "土壤温度", "℃", "🌱", "#8D6E63", "#EFEBE9"),
                new ST("soilHumidity", "土壤湿度", "%", "🪴", "#66BB6A", "#E8F5E9"),
                new ST("soilPh", "土壤pH值", "", "🧪", "#AB47BC", "#F3E5F5"),
                new ST("soilN", "土壤氮含量", "mg/kg", "🟢", "#4CAF50", "#E8F5E9"),
                new ST("soilP", "土壤磷含量", "mg/kg", "🔵", "#2196F3", "#E3F2FD"),
                new ST("soilK", "土壤钾含量", "mg/kg", "🟠", "#FF9800", "#FFF3E0"),
                new ST("windSpeed", "风速", "m/s", "💨", "#26C6DA", "#E0F7FA"),
                new ST("rainfall", "降雨量", "mm", "🌧️", "#5C6BC0", "#E8EAF6"),
                new ST("pressure", "大气压", "hPa", "🌐", "#78909C", "#ECEFF1"));

        for (ST s : types) {
            SensorType t = sensorTypeRepo.findBySensorKey(s.key()).orElseGet(SensorType::new);
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
        record ET(String key, String name, String icon, boolean ctrl, String controlType) {
        }

        List<ET> types = List.of(
                new ET("water_valve", "水阀", "🚿", true, "toggle"),
                new ET("water_pump", "水泵", "💦", true, "toggle"),
                new ET("led_light", "补光灯", "💡", true, "slider"),
                new ET("camera", "摄像头", "📷", false, "toggle"),
                new ET("fan", "排风扇", "🌀", true, "toggle"),
                new ET("heater", "加热器", "🔥", true, "toggle"),
                new ET("shade", "遮阳帘", "🪟", true, "toggle"),
                new ET("ventilation", "通风窗", "🪁", true, "toggle"),
                new ET("fertilizer_pump", "施肥泵", "🧴", true, "toggle"),
                new ET("mist_sprayer", "喷雾器", "🌫️", true, "toggle"),
                new ET("fertigation_machine", "水肥一体机", "🚿", true, "toggle"),
                new ET("air_source_ac", "空气能空调", "❄️", true, "toggle"),
                new ET("led_display", "LED显示屏", "📺", true, "toggle"),
                new ET("electric_skylight", "电动天窗", "🪟", true, "toggle"));

        for (ET e : types) {
            EquipmentType t = equipmentTypeRepo.findByEquipmentKey(e.key()).orElseGet(EquipmentType::new);
            t.setEquipmentKey(e.key());
            t.setName(e.name());
            t.setIcon(e.icon());
            t.setControllable(e.ctrl());
            t.setControlType(e.controlType());
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
        for (String key : List.of("water_valve", "water_pump", "led_light", "fan", "camera")) {
            NodeEquipment ne = new NodeEquipment();
            ne.setNodeId(nodeId);
            ne.setEquipmentKey(key);
            nodeEquipmentRepo.save(ne);
        }
    }

    /** SIM_Node_01: 温室全模拟传感器（10 种）+ 完整阈值 */
    private void seedSimNodeSensors(String nodeId) {
        record NS(String key, Double warnMin, Double warnMax) {
        }

        List<NS> sensors = List.of(
                new NS("temperature", 18.0, 32.0),
                new NS("humidity", 45.0, 75.0),
                new NS("co2", 350.0, 500.0),
                new NS("light", 200.0, 1600.0),
                new NS("soilTemp", 13.0, 27.0),
                new NS("soilHumidity", 55.0, 85.0),
                new NS("soilPh", 5.5, 8.0),
                new NS("soilN", 60.0, 280.0),
                new NS("soilP", 60.0, 280.0),
                new NS("soilK", 60.0, 280.0));

        for (NS s : sensors) {
            NodeSensor ns = new NodeSensor();
            ns.setNodeId(nodeId);
            ns.setSensorKey(s.key());
            ns.setWarningMin(s.warnMin());
            ns.setWarningMax(s.warnMax());
            nodeSensorRepo.save(ns);
        }
    }

    /** SIM_Node_01: 全量设备（14 种） */
    private void seedSimNodeEquipment(String nodeId) {
        for (String key : List.of("water_valve", "water_pump", "led_light", "camera",
                "fan", "heater", "shade", "ventilation", "fertilizer_pump", "mist_sprayer",
                "fertigation_machine", "air_source_ac", "led_display", "electric_skylight")) {
            NodeEquipment ne = new NodeEquipment();
            ne.setNodeId(nodeId);
            ne.setEquipmentKey(key);
            nodeEquipmentRepo.save(ne);
        }
    }

    /** 重新播种 SIM_Node_01 的传感器与设备绑定（热重启时修复旧配置） */
    private void reseedSimNodeBindings() {
        String nodeId = "SIM_Node_01";
        if (nodeRepo.findByNodeId(nodeId).isEmpty())
            return;

        // 清除旧绑定
        nodeSensorRepo.deleteByNodeId(nodeId);
        nodeEquipmentRepo.deleteByNodeId(nodeId);

        // 重新播种
        seedSimNodeSensors(nodeId);
        seedSimNodeEquipment(nodeId);
        log.info("[MockData] SIM_Node_01 传感器({})与设备绑定已重建。",
                nodeSensorRepo.findByNodeId(nodeId).size());
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

    private void seedOperationLogs(Long userId, String userName) {
        String[] keys = { "water_valve", "water_pump", "led_light", "fan", "camera" };
        String[][] actions = {
                { "turn_on", "开启" }, { "turn_off", "关闭" }
        };
        Random rng = new Random(99);
        long now = System.currentTimeMillis();
        int count = 0;
        for (String key : keys) {
            // 每种设备生成 4 条历史操作记录（交替开/关）
            for (int i = 0; i < 4; i++) {
                String[] act = actions[i % 2];
                String detail = String.format("{\"action\":\"%s\",\"mode\":\"manual\"}", act[0]);
                opLogService.log("DEVICE_CONTROL", userId, userName, "equipment", key, detail);
                count++;
            }
        }
        log.info("[MockData] 已生成 {} 条设备操作记录。", count);
    }
}
