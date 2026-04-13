package ww.wangnong.config;

import java.time.LocalDate;
import java.util.Optional;

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

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Purpose: 在所有运行模式下补齐最小可运行基础数据。
 * Responsibilities: 确保管理员账号、默认节点、传感器类型、设备类型以及关键静态绑定存在。
 * Flow: 应用启动早期执行 -> 检查各类基础数据 -> 缺失时增量补齐 -> 供后续 MockDataLifecycle 与业务服务复用。
 * Dependencies: User/Node/SensorType/EquipmentType
 * 相关仓库、RunModeHolder、PasswordEncoder。
 * Input: 当前数据库状态与运行模式。
 * Output: 已补全的基础种子数据。
 * Side Effects: 可能自动升级遗留管理员默认密码哈希，并创建默认节点及静态绑定。
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
    private final PasswordEncoder passwordEncoder;

    EssentialDataSeeder(UserRepository userRepo,
            NodeRepository nodeRepo,
            SensorTypeRepository sensorTypeRepo,
            EquipmentTypeRepository equipmentTypeRepo,
            NodeSensorRepository nodeSensorRepo,
            NodeEquipmentRepository nodeEquipmentRepo,
            RunModeHolder runModeHolder,
            PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.nodeRepo = nodeRepo;
        this.sensorTypeRepo = sensorTypeRepo;
        this.equipmentTypeRepo = equipmentTypeRepo;
        this.nodeSensorRepo = nodeSensorRepo;
        this.nodeEquipmentRepo = nodeEquipmentRepo;
        this.runModeHolder = runModeHolder;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean changed = false;
        changed |= ensureAdmin();
        changed |= ensureNodes();
        changed |= ensureSensorTypes();
        changed |= ensureEquipmentTypes();
        changed |= ensureSimNodeSensors();
        changed |= ensureSimNodeEquipment();
        changed |= ensureA03NodeEquipment();
        if (changed) {
            log.info("[EssentialData] 基础种子数据已补全。");
        }
    }

    private boolean ensureAdmin() {
        Optional<User> existingAdmin = userRepo.findByPhone("13800000001");
        if (existingAdmin.isPresent()) {
            User admin = existingAdmin.get();
            if (!requiresLegacyDefaultPasswordUpgrade(admin)) {
                return false;
            }
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            userRepo.save(admin);
            log.info("[EssentialData] 已自动升级管理员默认密码为 BCrypt 哈希。");
            return true;
        }
        User admin = new User();
        admin.setPhone("13800000001");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setUserName("管理员");
        admin.setGender("男");
        admin.setBirthDate(LocalDate.of(1985, 6, 15));
        admin.setRole("admin");
        userRepo.save(admin);
        log.info("[EssentialData] 已创建管理员用户 (13800000001)。");
        return true;
    }

    private boolean requiresLegacyDefaultPasswordUpgrade(User admin) {
        return "admin123".equals(admin.getPasswordHash());
    }

    private boolean ensureNodes() {
        record ND(String nodeId, String name) {
        }
        var defaultNodes = java.util.List.of(
                new ND("A03_Node_01", "温室节点 1"),
                new ND("A03_Node_02", "温室节点 2"),
                new ND("SIM_Node_01", "温室节点 3"));
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
        record ET(String key, String name, String icon, boolean ctrl, String controlType) {
        }
        var types = java.util.List.of(
                new ET("water_valve", "水阀", "🚿", true, "toggle"),
                new ET("water_pump", "水泵", "💦", true, "toggle"),
                new ET("led_light", "补光灯", "💡", true, "slider"),
                new ET("fan", "排风扇", "🌀", true, "toggle"),
                new ET("camera", "摄像头", "📷", false, "toggle"));
        boolean changed = false;
        for (var e : types) {
            EquipmentType t = equipmentTypeRepo.findByEquipmentKey(e.key()).orElseGet(EquipmentType::new);
            boolean created = t.getId() == null;
            boolean dirty = created;
            if (!java.util.Objects.equals(t.getName(), e.name())) {
                t.setName(e.name());
                dirty = true;
            }
            if (!java.util.Objects.equals(t.getIcon(), e.icon())) {
                t.setIcon(e.icon());
                dirty = true;
            }
            if (!java.util.Objects.equals(t.getControllable(), e.ctrl())) {
                t.setControllable(e.ctrl());
                dirty = true;
            }
            if (!java.util.Objects.equals(t.getControlType(), e.controlType())) {
                t.setControlType(e.controlType());
                dirty = true;
            }
            t.setEquipmentKey(e.key());
            if (dirty) {
                equipmentTypeRepo.save(t);
                changed = true;
            }
        }
        if (changed) {
            log.info("[EssentialData] 设备类型定义已补全/回填。", types.size());
        }
        return changed;
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

    private boolean ensureA03NodeEquipment() {
        var keys = java.util.List.of("water_valve", "water_pump", "led_light", "fan", "camera");
        boolean changed = false;
        changed |= ensureNodeEquipmentKeys("A03_Node_01", keys);
        changed |= ensureNodeEquipmentKeys("A03_Node_02", keys);
        return changed;
    }

    private boolean ensureSimNodeEquipment() {
        String nodeId = "SIM_Node_01";
        var keys = java.util.List.of("water_valve", "water_pump", "led_light", "fan", "camera");
        return ensureNodeEquipmentKeys(nodeId, keys);
    }

    /**
     * 确保指定节点拥有给定设备 key 的 NodeEquipment 记录，缺失则补建。
     */
    private boolean ensureNodeEquipmentKeys(String nodeId, java.util.List<String> keys) {
        var existing = nodeEquipmentRepo.findByNodeId(nodeId);
        var existingKeys = new java.util.HashSet<String>();
        for (var ne : existing) {
            existingKeys.add(ne.getEquipmentKey());
        }
        int added = 0;
        for (var key : keys) {
            if (!existingKeys.contains(key)) {
                NodeEquipment ne = new NodeEquipment();
                ne.setNodeId(nodeId);
                ne.setEquipmentKey(key);
                nodeEquipmentRepo.save(ne);
                added++;
            }
        }
        if (added > 0) {
            log.info("[EssentialData] 已为 {} 补建 {} 个设备关联。", nodeId, added);
        }
        return added > 0;
    }
}
