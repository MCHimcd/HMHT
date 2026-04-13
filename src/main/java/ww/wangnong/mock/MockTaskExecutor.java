package ww.wangnong.mock;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ww.wangnong.entity.ScheduledTask;
import ww.wangnong.mqtt.PahoMqttService;
import ww.wangnong.repository.ScheduledTaskRepository;
import ww.wangnong.service.OperationLogService;

/**
 * Purpose: 在 mock 模式下执行数据库中的定时任务定义。
 * Responsibilities: 按分钟扫描启用任务、匹配重复日规则、发布控制命令并记录操作日志。
 * Flow: executeDueTasks 定时触发 -> 读取 enabled 任务 -> 匹配 hour/minute/repeatDays ->
 * executeTask 发布 MQTT 命令。
 * Dependencies: ScheduledTaskRepository、PahoMqttService、OperationLogService。
 * Input: 当前时间与启用任务列表。
 * Output: 已发布的 mock command 与操作日志。
 * Boundary: 该执行器只在 MQTT 可用时执行任务，避免数据库状态与模拟设备状态脱节。
 */
@Component
@ConditionalOnProperty(name = "hmht.run.mode", havingValue = "mock", matchIfMissing = true)
public class MockTaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(MockTaskExecutor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ScheduledTaskRepository taskRepo;
    private final PahoMqttService mqttService;
    private final OperationLogService opLog;

    public MockTaskExecutor(ScheduledTaskRepository taskRepo,
            PahoMqttService mqttService,
            OperationLogService opLog) {
        this.taskRepo = taskRepo;
        this.mqttService = mqttService;
        this.opLog = opLog;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public void executeDueTasks() {
        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();
        int currentMinute = now.getMinute();
        int todayDow = now.getDayOfWeek().getValue(); // 1=Mon..7=Sun

        List<ScheduledTask> tasks = taskRepo.findByEnabledTrue();
        for (ScheduledTask task : tasks) {
            if (task.getHour() != currentHour || task.getMinute() != currentMinute) {
                continue;
            }
            if (!matchesDay(task.getRepeatDays(), todayDow)) {
                continue;
            }

            executeTask(task);
        }
    }

    private boolean matchesDay(String repeatDays, int todayDow) {
        if (repeatDays == null || repeatDays.isBlank()) {
            return true; // every day
        }
        for (String d : repeatDays.split(",")) {
            try {
                if (Integer.parseInt(d.trim()) == todayDow)
                    return true;
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    private void executeTask(ScheduledTask task) {
        if (!mqttService.isConnected()) {
            log.warn("[TaskExecutor] MQTT not connected, skipping task {}", task.getId());
            return;
        }

        try {
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("target_device", task.getEquipmentKey());
            action.put("command", task.getAction());
            action.put("source", "scheduled_task");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("msg_type", "command");
            payload.put("node_id", task.getNodeId());
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("action", action);

            String topic = "/wangnong_2026/node/" + task.getNodeId() + "/command";
            mqttService.publish(topic, MAPPER.writeValueAsString(payload), 1, false);

            log.info("[TaskExecutor] Executed task {} → {} {} on {}",
                    task.getId(), task.getEquipmentKey(), task.getAction(), task.getNodeId());

            opLog.log("DEVICE_CONTROL", task.getUserId(), null,
                    "equipment", task.getEquipmentKey(),
                    "定时任务[" + task.getTaskName() + "] → " + task.getAction());
        } catch (Exception e) {
            log.error("[TaskExecutor] Failed to execute task {}", task.getId(), e);
        }
    }
}
