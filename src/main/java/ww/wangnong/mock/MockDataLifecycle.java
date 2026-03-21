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
import ww.wangnong.entity.Device;
import ww.wangnong.entity.EquipmentMeta;
import ww.wangnong.entity.SensorMeta;
import ww.wangnong.entity.User;
import ww.wangnong.repository.DeviceRepository;
import ww.wangnong.repository.EquipmentMetaRepository;
import ww.wangnong.repository.SensorMetaRepository;
import ww.wangnong.repository.UserRepository;

@Component
@ConditionalOnProperty(name = "hmht.mock.seed.enabled", havingValue = "true", matchIfMissing = false)
public class MockDataLifecycle implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataLifecycle.class);

    private final UserRepository userRepo;
    private final DeviceRepository deviceRepo;
    private final SensorMetaRepository sensorMetaRepo;
    private final EquipmentMetaRepository equipmentMetaRepo;
    private final JdbcTemplate jdbc;

    public MockDataLifecycle(UserRepository userRepo, DeviceRepository deviceRepo,
                             SensorMetaRepository sensorMetaRepo,
                             EquipmentMetaRepository equipmentMetaRepo,
                             JdbcTemplate jdbc) {
        this.userRepo = userRepo;
        this.deviceRepo = deviceRepo;
        this.sensorMetaRepo = sensorMetaRepo;
        this.equipmentMetaRepo = equipmentMetaRepo;
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
        String[] tables = {"chat_messages", "command_logs", "alerts", "sensor_meta",
                "equipment_meta", "verification_codes", "devices", "users"};
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : tables) {
            jdbc.execute("TRUNCATE TABLE " + table);
        }
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
        log.info("[MockData] 数据库数据已清除。");
    }

    private void seed() {
        // 1. 用户
        User user = new User();
        user.setPhone("13800000000");
        user.setPasswordHash("123456");
        user.setUserName("农场主");
        user.setGender("男");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setAvatarSrc("https://example.com/avatar.jpg");
        user = userRepo.save(user);

        // 2. 设备
        seedDevice(user, "A03_Node_01", "温室节点 1");
        seedDevice(user, "A03_Node_02", "温室节点 2");

        // 3. 传感器元数据
        seedSensorMeta("A03_Node_01", true);
        seedSensorMeta("A03_Node_02", false);

        // 4. 执行器元数据
        seedEquipmentMeta("A03_Node_01");
        seedEquipmentMeta("A03_Node_02");
    }

    private void seedDevice(User user, String deviceId, String name) {
        Device d = new Device();
        d.setDeviceId(deviceId);
        d.setDeviceName(name);
        d.setUser(user);
        d.setOnline(true);
        d.setSmartMode(false);
        deviceRepo.save(d);
    }

    private void seedSensorMeta(String deviceId, boolean fullThresholds) {
        record SM(String key, String name, String unit, String icon,
                  String badgeColor, String bgColor, Double warnMin, Double warnMax) {}

        List<SM> sensors = List.of(
                new SM("temperature", "空气温度", "℃", "🌡️", "#FF7043", "#FFF3E0", 10.0, 35.0),
                new SM("humidity", "空气湿度", "%", "💧", "#42A5F5", "#E3F2FD", 30.0, 85.0),
                new SM("light", "光照强度", "Lux", "☀️", "#FDD835", "#FFFDE7", null, null),
                new SM("co2", "CO₂浓度", "ppm", "🫧", "#78909C", "#ECEFF1", null, 1500.0),
                new SM("soilTemp", "土壤温度", "℃", "🌱", "#8D6E63", "#EFEBE9",
                        fullThresholds ? 5.0 : null, fullThresholds ? 35.0 : null),
                new SM("soilHumidity", "土壤湿度", "%", "🪴", "#66BB6A", "#E8F5E9",
                        fullThresholds ? 20.0 : null, fullThresholds ? 80.0 : null),
                new SM("soilPh", "土壤pH", "", "⚗️", "#AB47BC", "#F3E5F5",
                        fullThresholds ? 5.5 : null, fullThresholds ? 7.5 : null),
                new SM("soilN", "土壤氮", "mg/kg", "🧪", "#26A69A", "#E0F2F1",
                        fullThresholds ? 15.0 : null, fullThresholds ? 100.0 : null),
                new SM("soilP", "土壤磷", "mg/kg", "🧫", "#5C6BC0", "#E8EAF6",
                        fullThresholds ? 10.0 : null, fullThresholds ? 60.0 : null),
                new SM("soilK", "土壤钾", "mg/kg", "⚙️", "#EF5350", "#FFEBEE",
                        fullThresholds ? 80.0 : null, fullThresholds ? 350.0 : null));

        for (SM s : sensors) {
            SensorMeta m = new SensorMeta();
            m.setDeviceId(deviceId);
            m.setSensorKey(s.key());
            m.setName(s.name());
            m.setUnit(s.unit());
            m.setIcon(s.icon());
            m.setBadgeColor(s.badgeColor());
            m.setBgColor(s.bgColor());
            m.setWarningMin(s.warnMin());
            m.setWarningMax(s.warnMax());
            sensorMetaRepo.save(m);
        }
    }

    private void seedEquipmentMeta(String deviceId) {
        record EM(String key, String name, String icon, boolean ctrl) {}

        List<EM> equips = List.of(
                new EM("water_pump", "水泵", "💦", true),
                new EM("led_light", "补光灯", "💡", true),
                new EM("fan", "风扇", "🌀", true),
                new EM("camera", "摄像头", "📷", false),
                new EM("heater", "加热器", "🔥", true),
                new EM("misting", "喷雾加湿", "🌫️", true),
                new EM("co2_generator", "CO₂发生器", "♨️", true),
                new EM("fertilizer", "施肥器", "🧴", true));

        for (EM e : equips) {
            EquipmentMeta m = new EquipmentMeta();
            m.setDeviceId(deviceId);
            m.setEquipmentKey(e.key());
            m.setName(e.name());
            m.setIcon(e.icon());
            m.setControllable(e.ctrl());
            equipmentMetaRepo.save(m);
        }
    }
}
