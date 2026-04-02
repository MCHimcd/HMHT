package ww.wangnong;

import java.time.LocalDateTime;

import ww.wangnong.entity.Alert;
import ww.wangnong.entity.ChatMessage;
import ww.wangnong.entity.CommandLog;
import ww.wangnong.entity.EquipmentType;
import ww.wangnong.entity.Node;
import ww.wangnong.entity.NodeEquipment;
import ww.wangnong.entity.NodeSensor;
import ww.wangnong.entity.OperationLog;
import ww.wangnong.entity.ScheduledTask;
import ww.wangnong.entity.SensorType;
import ww.wangnong.entity.User;
import ww.wangnong.entity.UserNodeAccess;
import ww.wangnong.entity.VerificationCode;

/**
 * Factory methods for creating test Entity instances with sensible defaults.
 * Only required fields are set; callers can customize further via setters.
 */
public final class TestDataFactory {

    private TestDataFactory() {
    }

    // ── User ──

    public static User user(String phone, String userName) {
        User u = new User();
        u.setPhone(phone);
        u.setPasswordHash("password123");
        u.setUserName(userName);
        u.setRole("user");
        return u;
    }

    public static User admin(String phone, String userName) {
        User u = user(phone, userName);
        u.setRole("admin");
        return u;
    }

    // ── Node ──

    public static Node node(String nodeId, String nodeName, User owner) {
        Node n = new Node();
        n.setNodeId(nodeId);
        n.setNodeName(nodeName);
        n.setUser(owner);
        n.setOnline(false);
        n.setSmartMode(false);
        return n;
    }

    // ── SensorType ──

    public static SensorType sensorType(String sensorKey, String name, String unit) {
        SensorType st = new SensorType();
        st.setSensorKey(sensorKey);
        st.setName(name);
        st.setUnit(unit);
        return st;
    }

    // ── NodeSensor ──

    public static NodeSensor nodeSensor(String nodeId, String sensorKey) {
        NodeSensor ns = new NodeSensor();
        ns.setNodeId(nodeId);
        ns.setSensorKey(sensorKey);
        return ns;
    }

    // ── EquipmentType ──

    public static EquipmentType equipmentType(String equipmentKey, String name) {
        EquipmentType et = new EquipmentType();
        et.setEquipmentKey(equipmentKey);
        et.setName(name);
        et.setControllable(true);
        return et;
    }

    // ── NodeEquipment ──

    public static NodeEquipment nodeEquipment(String nodeId, String equipmentKey) {
        NodeEquipment ne = new NodeEquipment();
        ne.setNodeId(nodeId);
        ne.setEquipmentKey(equipmentKey);
        return ne;
    }

    // ── Alert ──

    public static Alert alert(String alertId, String nodeId) {
        Alert a = new Alert();
        a.setAlertId(alertId);
        a.setNodeId(nodeId);
        a.setType("temperature");
        a.setTypeName("温度异常");
        a.setLevel("warning");
        a.setMessage("温度超出阈值");
        a.setSource("sensor");
        a.setSourceName("温度传感器");
        a.setIsHandled(false);
        a.setAlertTime(LocalDateTime.now());
        return a;
    }

    // ── ScheduledTask ──

    public static ScheduledTask scheduledTask(String nodeId, Long userId) {
        ScheduledTask t = new ScheduledTask();
        t.setNodeId(nodeId);
        t.setUserId(userId);
        t.setEquipmentKey("water_pump");
        t.setAction("turn_on");
        t.setTaskName("定时灌溉");
        t.setHour(8);
        t.setMinute(0);
        t.setRepeatDays("1,2,3,4,5");
        t.setEnabled(true);
        return t;
    }

    // ── OperationLog ──

    public static OperationLog operationLog(String operationType, Long userId) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUserName("testUser");
        log.setOperationType(operationType);
        log.setTargetType("node");
        log.setTargetId("N01");
        log.setDetail("测试操作");
        return log;
    }

    // ── UserNodeAccess ──

    public static UserNodeAccess userNodeAccess(Long userId, String nodeId) {
        UserNodeAccess una = new UserNodeAccess();
        una.setUserId(userId);
        una.setNodeId(nodeId);
        return una;
    }

    // ── VerificationCode ──

    public static VerificationCode verificationCode(String phone, String code) {
        VerificationCode vc = new VerificationCode();
        vc.setPhone(phone);
        vc.setCode(code);
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        vc.setUsed(false);
        return vc;
    }

    // ── CommandLog ──

    public static CommandLog commandLog(String nodeId, String command) {
        CommandLog cl = new CommandLog();
        cl.setNodeId(nodeId);
        cl.setTargetDevice("water_pump");
        cl.setCommand(command);
        cl.setSource("app");
        return cl;
    }

    // ── ChatMessage ──

    public static ChatMessage chatMessage(User user, String role, String content) {
        ChatMessage cm = new ChatMessage();
        cm.setUser(user);
        cm.setRole(role);
        cm.setContent(content);
        return cm;
    }
}
