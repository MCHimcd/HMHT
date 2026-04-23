package ww.wangnong.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ww.wangnong.entity.Alert;
import ww.wangnong.entity.EquipmentType;
import ww.wangnong.entity.Node;
import ww.wangnong.entity.NodeEquipment;
import ww.wangnong.entity.NodeSensor;
import ww.wangnong.entity.OperationLog;
import ww.wangnong.entity.ScheduledTask;
import ww.wangnong.entity.SensorType;
import ww.wangnong.entity.User;
import ww.wangnong.entity.UserNodeAccess;
import ww.wangnong.repository.AlertRepository;
import ww.wangnong.repository.EquipmentTypeRepository;
import ww.wangnong.repository.NodeEquipmentRepository;
import ww.wangnong.repository.NodeRepository;
import ww.wangnong.repository.NodeSensorRepository;
import ww.wangnong.repository.OperationLogRepository;
import ww.wangnong.repository.ScheduledTaskRepository;
import ww.wangnong.repository.SensorTypeRepository;
import ww.wangnong.repository.UserNodeAccessRepository;
import ww.wangnong.repository.UserRepository;

/**
 * Purpose: 为课程版应用补齐固定演示数据。
 * Responsibilities: 确保默认用户、单个模拟节点、固定传感器/设备字典、节点绑定、告警、任务和操作日志存在。
 * Flow: 应用启动 -> 逐项幂等检查 -> 缺失时写入最小演示数据。
 * Dependencies: 课程版核心实体仓库、PasswordEncoder。
 * Input: 当前 MySQL 数据库状态。
 * Output: 可直接演示的课程版基础数据。
 * Side Effects: 首次启动时写入演示数据；后续启动只补齐缺失项。
 */
@Component
@Order(0)
class EssentialDataSeeder implements ApplicationRunner {

    static final String DEMO_NODE_ID = "SIM_Node_01";
    static final String DEMO_USER_PHONE = "13800000000";
    static final String ADMIN_PHONE = "13800000001";

    private static final Logger log = LoggerFactory.getLogger(EssentialDataSeeder.class);

    private final UserRepository userRepo;
    private final NodeRepository nodeRepo;
    private final SensorTypeRepository sensorTypeRepo;
    private final EquipmentTypeRepository equipmentTypeRepo;
    private final NodeSensorRepository nodeSensorRepo;
    private final NodeEquipmentRepository nodeEquipmentRepo;
    private final UserNodeAccessRepository userNodeAccessRepo;
    private final AlertRepository alertRepo;
    private final ScheduledTaskRepository taskRepo;
    private final OperationLogRepository operationLogRepo;
    private final PasswordEncoder passwordEncoder;

    EssentialDataSeeder(UserRepository userRepo,
            NodeRepository nodeRepo,
            SensorTypeRepository sensorTypeRepo,
            EquipmentTypeRepository equipmentTypeRepo,
            NodeSensorRepository nodeSensorRepo,
            NodeEquipmentRepository nodeEquipmentRepo,
            UserNodeAccessRepository userNodeAccessRepo,
            AlertRepository alertRepo,
            ScheduledTaskRepository taskRepo,
            OperationLogRepository operationLogRepo,
            PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.nodeRepo = nodeRepo;
        this.sensorTypeRepo = sensorTypeRepo;
        this.equipmentTypeRepo = equipmentTypeRepo;
        this.nodeSensorRepo = nodeSensorRepo;
        this.nodeEquipmentRepo = nodeEquipmentRepo;
        this.userNodeAccessRepo = userNodeAccessRepo;
        this.alertRepo = alertRepo;
        this.taskRepo = taskRepo;
        this.operationLogRepo = operationLogRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        User admin = ensureUser(ADMIN_PHONE, "管理员", "admin", "admin123");
        User demoUser = ensureUser(DEMO_USER_PHONE, "农场主", "user", "123456");
        ensureSingleDemoNode(demoUser);
        ensureSensorTypes();
        ensureEquipmentTypes();
        ensureNodeSensors();
        ensureNodeEquipment();
        ensureDemoUserAccess(demoUser);
        ensureAlerts();
        ensureScheduledTask(demoUser);
        ensureOperationLog(admin);
        log.info("[CourseData] 课程版固定演示数据已检查完成。");
    }

    private User ensureUser(String phone, String userName, String role, String password) {
        Optional<User> existing = userRepo.findByPhone(phone);
        if (existing.isPresent()) {
            User user = existing.get();
            if (password.equals(user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(password));
                userRepo.save(user);
            }
            return user;
        }

        User user = new User();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUserName(userName);
        user.setGender("男");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setRole(role);
        return userRepo.save(user);
    }

    private void ensureSingleDemoNode(User owner) {
        Node node = nodeRepo.findByNodeId(DEMO_NODE_ID).orElseGet(Node::new);
        node.setNodeId(DEMO_NODE_ID);
        node.setNodeName("课程模拟温室节点");
        node.setUser(owner);
        node.setOnline(true);
        node.setSmartMode(false);
        nodeRepo.save(node);
    }

    private void ensureSensorTypes() {
        record ST(String key, String name, String unit, String icon, String badgeColor, String bgColor) {
        }
        List<ST> types = List.of(
                new ST("temperature", "空气温度", "C", "temp", "#FF7043", "#FFF3E0"),
                new ST("humidity", "空气湿度", "%", "humidity", "#42A5F5", "#E3F2FD"),
                new ST("light", "光照强度", "Lux", "light", "#FDD835", "#FFFDE7"),
                new ST("soilHumidity", "土壤湿度", "%", "soil", "#66BB6A", "#E8F5E9"));
        for (ST s : types) {
            SensorType type = sensorTypeRepo.findBySensorKey(s.key()).orElseGet(SensorType::new);
            type.setSensorKey(s.key());
            type.setName(s.name());
            type.setUnit(s.unit());
            type.setIcon(s.icon());
            type.setBadgeColor(s.badgeColor());
            type.setBgColor(s.bgColor());
            sensorTypeRepo.save(type);
        }
    }

    private void ensureEquipmentTypes() {
        record ET(String key, String name, String icon, boolean controllable, String controlType) {
        }
        List<ET> types = List.of(
                new ET("water_pump", "水泵", "pump", true, "toggle"),
                new ET("led_light", "补光灯", "light", true, "slider"),
                new ET("fan", "排风扇", "fan", true, "toggle"));
        for (ET e : types) {
            EquipmentType type = equipmentTypeRepo.findByEquipmentKey(e.key()).orElseGet(EquipmentType::new);
            type.setEquipmentKey(e.key());
            type.setName(e.name());
            type.setIcon(e.icon());
            type.setControllable(e.controllable());
            type.setControlType(e.controlType());
            equipmentTypeRepo.save(type);
        }
    }

    private void ensureNodeSensors() {
        record NS(String key, Double warningMin, Double warningMax) {
        }
        List<NS> sensors = List.of(
                new NS("temperature", 15.0, 35.0),
                new NS("humidity", 30.0, 85.0),
                new NS("light", 100.0, 50000.0),
                new NS("soilHumidity", 20.0, 80.0));
        for (NS s : sensors) {
            NodeSensor nodeSensor = nodeSensorRepo.findByNodeIdAndSensorKey(DEMO_NODE_ID, s.key()).orElseGet(NodeSensor::new);
            nodeSensor.setNodeId(DEMO_NODE_ID);
            nodeSensor.setSensorKey(s.key());
            nodeSensor.setWarningMin(s.warningMin());
            nodeSensor.setWarningMax(s.warningMax());
            nodeSensorRepo.save(nodeSensor);
        }
    }

    private void ensureNodeEquipment() {
        for (String key : List.of("water_pump", "led_light", "fan")) {
            boolean exists = nodeEquipmentRepo.findByNodeId(DEMO_NODE_ID).stream()
                    .anyMatch(item -> key.equals(item.getEquipmentKey()));
            if (!exists) {
                NodeEquipment equipment = new NodeEquipment();
                equipment.setNodeId(DEMO_NODE_ID);
                equipment.setEquipmentKey(key);
                nodeEquipmentRepo.save(equipment);
            }
        }
    }

    private void ensureDemoUserAccess(User demoUser) {
        if (!userNodeAccessRepo.existsByUserIdAndNodeId(demoUser.getId(), DEMO_NODE_ID)) {
            UserNodeAccess access = new UserNodeAccess();
            access.setUserId(demoUser.getId());
            access.setNodeId(DEMO_NODE_ID);
            userNodeAccessRepo.save(access);
        }
    }

    private void ensureAlerts() {
        if (alertRepo.findByAlertId("COURSE_ALERT_1").isPresent()) {
            return;
        }
        Alert alert = new Alert();
        alert.setAlertId("COURSE_ALERT_1");
        alert.setNodeId(DEMO_NODE_ID);
        alert.setType("temperature");
        alert.setTypeName("空气温度");
        alert.setLevel("warning");
        alert.setMessage("模拟温室空气温度偏高，请检查通风设备。");
        alert.setSource("system");
        alert.setSourceName("课程版演示数据");
        alert.setIsHandled(false);
        alert.setAlertTime(LocalDateTime.now().minusMinutes(20));
        alertRepo.save(alert);
    }

    private void ensureScheduledTask(User demoUser) {
        if (!taskRepo.findByUserIdOrderByCreatedAtDesc(demoUser.getId()).isEmpty()) {
            return;
        }
        ScheduledTask task = new ScheduledTask();
        task.setNodeId(DEMO_NODE_ID);
        task.setUserId(demoUser.getId());
        task.setEquipmentKey("water_pump");
        task.setAction("turn_on");
        task.setTaskName("每日灌溉");
        task.setHour(8);
        task.setMinute(30);
        task.setRepeatDays("1,2,3,4,5,6,7");
        task.setEnabled(true);
        taskRepo.save(task);
    }

    private void ensureOperationLog(User admin) {
        if (operationLogRepo.count() > 0) {
            return;
        }
        OperationLog logItem = new OperationLog();
        logItem.setUserId(admin.getId());
        logItem.setUserName(admin.getUserName());
        logItem.setOperationType("SYSTEM_SEED");
        logItem.setTargetType("course_data");
        logItem.setTargetId(DEMO_NODE_ID);
        logItem.setDetail("初始化课程版演示数据");
        operationLogRepo.save(logItem);
    }
}
