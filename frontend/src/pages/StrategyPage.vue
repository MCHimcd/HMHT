<script setup lang="ts">
import { CalendarClock, RefreshCcw, ShieldCheck, Sparkles } from "lucide-vue-next";
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import { listAlerts } from "../api/alerts";
import { listTasks } from "../api/tasks";
import type { AlertList, TaskInfo } from "../api/types";
import DataState from "../components/DataState.vue";
import { defaultNodeId, equipmentLabel } from "../utils/domain";
import { errorMessage, formatRepeatDays, formatTime } from "../utils/format";

const status = ref<"idle" | "loading" | "ready" | "empty" | "error">("idle");
const error = ref("");
const tasks = ref<TaskInfo[]>([]);
const alerts = ref<AlertList | null>(null);

const strategyCards = computed(() => [
  {
    title: "温湿协同策略",
    detail: "当空气温度偏高或湿度连续上升时，优先打开排风扇进行缓释。",
    tag: `${alerts.value?.unhandled_count ?? 0} 条待关注`,
  },
  {
    title: "光照补偿策略",
    detail: "根据最近光照走势，在清晨和傍晚平滑提升补光灯强度。",
    tag: `${tasks.value.filter((task) => task.equipmentKey === "led_light").length} 个相关任务`,
  },
  {
    title: "灌溉节律策略",
    detail: "结合土壤湿度曲线和既有排程，避免连续灌溉造成波动。",
    tag: `${tasks.value.filter((task) => task.equipmentKey === "water_pump").length} 个相关任务`,
  },
]);

async function load() {
  status.value = "loading";
  error.value = "";
  try {
    const [taskResult, alertResult] = await Promise.all([
      listTasks(),
      listAlerts({ nodeId: defaultNodeId, handled: null, page: 1, size: 5 }),
    ]);
    tasks.value = taskResult;
    alerts.value = alertResult;
    status.value = "ready";
  } catch (err) {
    error.value = errorMessage(err);
    status.value = "error";
  }
}

onMounted(load);
</script>

<template>
  <div class="page-stack">
    <header class="page-head">
      <div>
        <p class="eyebrow">Strategy</p>
        <h1>自动化策略</h1>
        <p class="muted">把规则和排程分开展示：这里看策略逻辑，任务调度页负责具体时刻。</p>
      </div>
      <button class="button" type="button" @click="load"><RefreshCcw :size="17" />刷新</button>
    </header>

    <DataState :status="status" :message="error || '暂无策略数据'" @retry="load">
      <section class="grid three">
        <article v-for="item in strategyCards" :key="item.title" class="card pad strategy-card">
          <div class="strategy-head">
            <Sparkles :size="18" />
            <span class="badge teal">{{ item.tag }}</span>
          </div>
          <h2>{{ item.title }}</h2>
          <p class="muted">{{ item.detail }}</p>
        </article>
      </section>

      <section class="grid two">
        <article class="card pad">
          <div class="toolbar">
            <h2>告警联动建议</h2>
            <span class="badge warning"><ShieldCheck :size="14" />{{ alerts?.unhandled_count ?? 0 }} 条未处理</span>
          </div>
          <ul class="strategy-list">
            <li v-for="alert in alerts?.items || []" :key="alert.alert_id">
              <strong>{{ alert.type_name }}</strong>
              <span>{{ alert.message }}</span>
            </li>
          </ul>
        </article>

        <article class="card pad">
          <div class="toolbar">
            <h2>策略落地任务</h2>
            <RouterLink class="button primary" to="/tasks"><CalendarClock :size="16" />编辑排程</RouterLink>
          </div>
          <ul class="strategy-list">
            <li v-for="task in tasks.slice(0, 4)" :key="task.id">
              <strong>{{ task.taskName }}</strong>
              <span>{{ equipmentLabel(task.equipmentKey) }} · {{ formatTime(task.hour, task.minute) }} · 周{{ formatRepeatDays(task.repeatDays) }}</span>
            </li>
          </ul>
        </article>
      </section>
    </DataState>
  </div>
</template>

<style scoped>
.strategy-card {
  display: grid;
  gap: 12px;
}

.strategy-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--green);
}

.strategy-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.strategy-list li {
  display: grid;
  gap: 5px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
  background: var(--surface);
}

.strategy-list strong {
  font-size: 15px;
}

.strategy-list span {
  color: var(--muted);
  font-size: 13px;
}
</style>
