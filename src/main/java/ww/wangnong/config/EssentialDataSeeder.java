package ww.wangnong.config;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

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

import ww.wangnong.config.RunMode;
import ww.wangnong.config.RunModeHolder;

/**
 * 全模式通用种子数据：确保 admin 用户、传感器类型、设备类型始终存在。
 * 在所有运行模式下都执行，使用 {@code existsBy*} 避免重复插入。
 * Order(0) 确保在 MockDataLifecycle 之前运行。
 */
@Component
@Order(0)
class EssentialDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EssentialDataSeeder.class);

    private final UserRepository userRepo;
    private final NodeRepository nodeRepo;
    private final SensorTypeRepository sensorTypeRepo;
    private final EquipmentTypeRepository equipmentTypeRepo;
    private final NodeSensorRepository nodeSensorRepo;
    private final NodeEquipmentRepository nodeEquipmentRepo;
    private final RunModeHolder runModeHolder;

    EssentialDataSeeder(UserRepository userRepo,
            NodeRepository nodeRepo,
            SensorTypeRepository sensorTypeRepo,
            EquipmentTypeRepository equipmentTypeRepo,
            NodeSensorRepository nodeSensorRepo,
            NodeEquipmentRepository nodeEquipmentRepo,
            RunModeHolder runModeHolder) {
        this.userRepo = userRepo;
        this.nodeRepo = nodeRepo;
        this.sensorTypeRepo = sensorTypeRepo;
        this.equipmentTypeRepo = equipmentTypeRepo;
        this.nodeSensorRepo = nodeSensorRepo;
        this.nodeEquipmentRepo = nodeEquipmentRepo;
        this.runModeHolder = runModeHolder;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean changed = false;
        changed |= ensureAdmin();
        changed |= ensureNodes();
        changed |= ensureSensorTypes();
        changed |= ensureEquipmentTypes();
        if (runModeHolder.getMode() != RunMode.HARDWARE) {
            changed |= ensureSimNodeSensors();
            changed |= ensureSimNodeEquipment();
        }
        if (changed) {
            log.info("[EssentialData] 基础种子数据已补全。");
        }
    }

    private boolean ensureAdmin() {
        if (userRepo.existsByPhone("13800000001")) {
            return false;
        }
        User admin = new User();
        admin.setPhone("13800000001");
        admin.setPasswordHash("admin123");
        admin.setUserName("管理员");
        admin.setGender("男");
        admin.setBirthDate(LocalDate.of(1985, 6, 15));
        admin.setRole("admin");
        userRepo.save(admin);
        log.info("[EssentialData] 已创建管理员用户 (13800000001)。");
        return true;
    }

    private boolean ensureNodes() {
        record ND(String nodeId, String name) {
        }
        var defaultNodes = java.util.List.of(
                new ND("A03_Node_01", "温室节点 1"),
                new ND("A03_Node_02", "温室节点 2"));
        if (runModeHolder.getMode() != RunMode.HARDWARE) {
            defaultNodes = java.util.List.of(
                    new ND("A03_Node_01", "温室节点 1"),
                    new ND("A03_Node_02", "温室节点 2"),
                    new ND("SIM_Node_01", "全模拟演示节点"));
        }
        boolean created = false;
        for (var nd : defaultNodes) {
            if (nodeRepo.findByNodeId(nd.nodeId()).isEmpty()) {
                Node node = new Node();
                node.setNodeId(nd.nodeId());
                node.setNodeName(nd.name());
                node.setOnline(false);
                node.setSmartMode(false);
                nodeRepo.save(node);
                created = true;
            }
        }
        if (created) {
            log.info("[EssentialData] 已创建默认节点。");
        }
        return created;
    }

    private boolean ensureSensorTypes() {
        if (sensorTypeRepo.count() > 0) {
            return false;
        }
        record ST(String key, String name, String unit, String icon, String badgeColor, String bgColor) {
        }
        var types = java.util.List.of(
                new ST("temperature", "空气温度", "℃", "🌡️", "#FF7043", "#FFF3E0"),
                new ST("humidity", "空气湿度", "%", "💧", "#42A5F5", "#E3F2FD"),
                new ST("co2", "CO₂浓度", "ppm", "🫧", "#78909C", "#ECEFF1"),
                new ST("light", "光照强度", "Lux", "☀️", "#FDD835", "#FFFDE7"),
                new ST("soilTemp", "土壤温度", "℃", "🌱", "#8D6E63", "#EFEBE9"),
                new ST("soilHumidity", "土壤湿度", "%", "🪴", "#66BB6A", "#E8F5E9"));
        for (var s : types) {
            SensorType t = new SensorType();
            t.setSensorKey(s.key());
            t.setName(s.name());
            t.setUnit(s.unit());
            t.setIcon(s.icon());
            t.setBadgeColor(s.badgeColor());
            t.setBgColor(s.bgColor());
            sensorTypeRepo.save(t);
        }
        log.info("[EssentialData] 已创建 {} 种传感器类型。", types.size());
        return true;
    }

    private boolean ensureEquipmentTypes() {
        if (equipmentTypeRepo.count() > 0) {
            return false;
        }
        record ET(String key, String name, String icon, boolean ctrl) {
        }
        var types = java.util.List.of(
                new ET("water_valve", "水阀", "🚿", true),
                new ET("water_pump", "水泵", "💦", true),
                new ET("led_light", "补光灯", "💡", true),
                new ET("camera", "摄像头", "📷", false));
        for (var e : types) {
            EquipmentType t = new EquipmentType();
            t.setEquipmentKey(e.key());
            t.setName(e.name());
            t.setIcon(e.icon());
            t.setControllable(e.ctrl());
            equipmentTypeRepo.save(t);
        }
        log.info("[EssentialData] 已创建 {} 种设备类型。", types.size());
        return true;
    }

    /**
     * 确保 SIM_Node_01 拥有 node_sensor 记录（含阈值），使 TelemetryAlertChecker 可触发告警。
     */
    private boolean ensureSimNodeSensors() {
        String nodeId = "SIM_Node_01";
        if (!nodeSensorRepo.findByNodeId(nodeId).isEmpty()) {
            return false;
        }
        record NS(String key, Double warnMin, Double warnMax) {
        }
        var sensors = java.util.List.of(
                new NS("temperature", 10.0, 35.0),
                new NS("humidity", 30.0, 85.0),
                new NS("co2", 300.0, 1500.0),
                new NS("light", 1000.0, 50000.0),
                new NS("soilTemp", 5.0, 35.0),
                new NS("soilHumidity", 20.0, 80.0));
        for (var s : sensors) {
            NodeSensor ns = new NodeSensor();
            ns.setNodeId(nodeId);
            ns.setSensorKey(s.key());
            ns.setWarningMin(s.warnMin());
            ns.setWarningMax(s.warnMax());
            nodeSensorRepo.save(ns);
        }
        log.info("[EssentialData] 已创建 SIM_Node_01 的 {} 个传感器阈值。", sensors.size());
        return true;
    }

    private boolean ensureSimNodeEquipment() {
        String nodeId = "SIM_Node_01";
        if (!nodeEquipmentRepo.findByNodeId(nodeId).isEmpty()) {
            return false;
        }
        var keys = java.util.List.of("water_valve", "water_pump", "led_light", "camera");
        for (var key : keys) {
            NodeEquipment ne = new NodeEquipment();
            ne.setNodeId(nodeId);
            ne.setEquipmentKey(key);
            nodeEquipmentRepo.save(ne);
        }
        log.info("[EssentialData] 已创建 SIM_Node_01 的 {} 个设备关联。", keys.size());
        return true;
    }
}
