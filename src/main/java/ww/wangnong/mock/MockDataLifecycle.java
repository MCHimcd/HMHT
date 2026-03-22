package ww.wangnong.mock;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import ww.wangnong.entity.EquipmentType;
import ww.wangnong.entity.Node;
import ww.wangnong.entity.NodeEquipment;
import ww.wangnong.entity.NodeSensor;
import ww.wangnong.entity.SensorType;
import ww.wangnong.entity.User;
import ww.wangnong.repository.EquipmentTypeRepository;
import ww.wangnong.repository.NodeEquipmentRepository;
import ww.wangnong.repository.NodeRepository;
import ww.wangnong.repository.NodeSensorRepository;
import ww.wangnong.repository.SensorTypeRepository;
import ww.wangnong.repository.UserRepository;

@Component
@ConditionalOnProperty(name = "hmht.mock.seed.enabled", havingValue = "true", matchIfMissing = false)
public class MockDataLifecycle implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataLifecycle.class);

    private final UserRepository userRepo;
    private final NodeRepository nodeRepo;
    private final SensorTypeRepository sensorTypeRepo;
    private final NodeSensorRepository nodeSensorRepo;
    private final EquipmentTypeRepository equipmentTypeRepo;
    private final NodeEquipmentRepository nodeEquipmentRepo;
    private final JdbcTemplate jdbc;

    public MockDataLifecycle(UserRepository userRepo, NodeRepository nodeRepo,
                             SensorTypeRepository sensorTypeRepo,
                             NodeSensorRepository nodeSensorRepo,
                             EquipmentTypeRepository equipmentTypeRepo,
                             NodeEquipmentRepository nodeEquipmentRepo,
                             JdbcTemplate jdbc) {
        this.userRepo = userRepo;
        this.nodeRepo = nodeRepo;
        this.sensorTypeRepo = sensorTypeRepo;
        this.nodeSensorRepo = nodeSensorRepo;
        this.equipmentTypeRepo = equipmentTypeRepo;
        this.nodeEquipmentRepo = nodeEquipmentRepo;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepo.count() > 0) {
            log.info("[MockData] 数据已存在，跳过初始化。");
            return;
        }
        seed();
        log.info("[MockData] 模拟数据初始化完成。");
    }

    @PreDestroy
    public void cleanup() {
        log.info("[MockData] 正在清除数据库数据...");
        String[] tables = {"chat_messages", "command_logs", "alerts", "node_sensor",
                "node_equipment", "sensor_type", "equipment_type",
                "verification_codes", "nodes", "users"};
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : tables) {
            jdbc.execute("TRUNCATE TABLE " + table);
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
        record ST(String key, String name, String unit, String icon, String badgeColor, String bgColor) {}

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
        record ET(String key, String name, String icon, boolean ctrl) {}

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
        record NS(String key, Double warnMin, Double warnMax) {}

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
}
