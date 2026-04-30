<script setup lang="ts">
import {
  Activity,
  BellRing,
  Clock3,
  Droplets,
  Gauge,
  RefreshCw,
  ShieldCheck,
  SlidersHorizontal,
  Stethoscope,
  SunMedium,
  Thermometer,
  Waves,
  Wifi,
} from "lucide-vue-next";
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";

import { handleAlert, listAlerts } from "../api/alerts";
import { getEquipmentMeta, getNode, getSensorMeta } from "../api/nodes";
import { getSensorHistory } from "../api/sensors";
import { listTasks } from "../api/tasks";
import type {
  AlertInfo,
  AlertList,
  EquipmentMeta,
  HistoryPoint,
  NodeDetail,
  SensorHistory,
  SensorMeta,
  SensorMetric,
  TaskInfo,
} from "../api/types";
import ChartPanel from "../components/ChartPanel.vue";
import DataState from "../components/DataState.vue";
import { alertCount, buildDeviceRuntimeSummary, formatMetricValue, latestMetricValue } from "../utils/console";
import { actionLabel, equipmentLabel, sensorOptions } from "../utils/domain";
import { errorMessage, formatDateTime, formatRepeatDays, formatTime } from "../utils/format";

const route = useRoute();
const nodeId = computed(() => String(route.params.nodeId));
const status = ref<"idle" | "loading" | "ready" | "empty" | "error">("idle");
const error = ref("");
const node = ref<NodeDetail | null>(null);
const sensors = ref<SensorMeta[]>([]);
const equipment = ref<EquipmentMeta[]>([]);
const histories = ref<Partial<Record<SensorMetric, SensorHistory>>>({});
const alerts = ref<AlertList | null>(null);
const tasks = ref<TaskInfo[]>([]);
const activeAlertTab = ref<"all" | "unhandled" | "handled">("all");
const handlingAlertId = ref("");
const lastLoadedAt = ref(Date.now());
const now = ref(Date.now());
let refreshTimer: number | undefined;
let tickTimer: number | undefined;

const trendColors: Record<SensorMetric, string> = {
  temperature: "#54b948",
  humidity: "#278af0",
  light: "#f5a400",
  soilHumidity: "#22b8b8",
};

const sensorIconMap = {
  temperature: Thermometer,
  humidity: Droplets,
  light: SunMedium,
  soilHumidity: Waves,
} as const;

const trendSeries = computed(() =>
  sensorOptions
    .map((option) => ({
      name: `${option.label}${option.unit ? `(${option.unit})` : ""}`,
      unit: option.unit,
      color: trendColors[option.key],
      axis: option.key === "light" ? ("secondary" as const) : ("primary" as const),
      points: histories.value[option.key]?.points ?? [],
    }))
    .filter((item) => item.points.length),
);

const emptyPoints = computed<HistoryPoint[]>(() => trendSeries.value[0]?.points ?? []);

const unhandledAlerts = computed(() => alerts.value?.items.filter((item) => !item.is_handled) ?? []);
const handledAlerts = computed(() => alerts.value?.items.filter((item) => item.is_handled) ?? []);
const filteredAlerts = computed(() => {
  if (activeAlertTab.value === "unhandled") return unhandledAlerts.value;
  if (activeAlertTab.value === "handled") return handledAlerts.value;
  return alerts.value?.items ?? [];
});

const todayTasks = computed(() => tasks.value.slice(0, 4));
const deviceSummary = computed(() => buildDeviceRuntimeSummary(equipment.value, unhandledAlerts.value.length > 0));
const healthScore = computed(() => Math.max(80, 96 - unhandledAlerts.value.length * 4));
const runningPercent = computed(() =>
  deviceSummary.value.total ? Math.round((deviceSummary.value.running / deviceSummary.value.total) * 100) : 0,
);
const lastReportLabel = computed(() => `${Math.max(1, Math.round((now.value - lastLoadedAt.value) / 1000))} 秒前`);
const updatedAt = computed(() => formatDateTime(lastLoadedAt.value));

const summaryCards = computed(() => [
  {
    label: "节点健康度",
    value: String(healthScore.value),
    suffix: "分",
    helper: "健康",
    icon: ShieldCheck,
    tone: "green",
    progress: healthScore.value,
  },
  {
    label: "运行时长",
    value: "18",
    suffix: "天 7 小时",
    helper: "自课程数据初始化后",
    icon: Clock3,
    tone: "blue",
    progress: 78,
  },
  {
    label: "数据上报",
    value: lastReportLabel.value.replace(" 秒前", ""),
    suffix: "秒前",
    helper: `最近刷新 ${lastReportLabel.value}`,
    icon: Activity,
    tone: "blue",
    progress: 68,
  },
  {
    label: "今日告警",
    value: String(alertCount(alerts.value)),
    suffix: "条",
    helper: `未处理 ${unhandledAlerts.value.length}`,
    icon: BellRing,
    tone: "orange",
    progress: Math.min(100, 24 + unhandledAlerts.value.length * 18),
  },
  {
    label: "关联设备",
    value: String(deviceSummary.value.total),
    suffix: "台",
    helper: `运行 ${deviceSummary.value.running} / 空闲 ${deviceSummary.value.idle}`,
    icon: Stethoscope,
    tone: "teal",
    progress: runningPercent.value,
  },
]);

const sensorCards = computed(() =>
  sensorOptions.map((option) => ({
    ...option,
    icon: sensorIconMap[option.key],
    value: formatMetricValue(latestMetricValue(histories.value, option.key), option.key),
  })),
);

function deviceRate(count: number) {
  return deviceSummary.value.total ? Math.round((count / deviceSummary.value.total) * 100) : 0;
}

async function loadLiveData() {
  const [alertResult, historyEntries] = await Promise.all([
    listAlerts({ nodeId: nodeId.value, handled: null, page: 1, size: 8 }),
    Promise.all(
      sensorOptions.map(async (option) => [option.key, await getSensorHistory(nodeId.value, option.key, "24h")] as const),
    ),
  ]);
  alerts.value = alertResult;
  histories.value = Object.fromEntries(historyEntries) as Partial<Record<SensorMetric, SensorHistory>>;
  lastLoadedAt.value = Date.now();
}

async function load() {
  status.value = "loading";
  error.value = "";
  try {
    const [nodeResult, sensorResult, equipmentResult, taskResult] = await Promise.all([
      getNode(nodeId.value),
      getSensorMeta(nodeId.value),
      getEquipmentMeta(nodeId.value),
      listTasks(),
    ]);
    node.value = nodeResult;
    sensors.value = sensorResult;
    equipment.value = equipmentResult;
    tasks.value = taskResult;
    await loadLiveData();
    status.value = "ready";
  } catch (err) {
    error.value = errorMessage(err);
    status.value = "error";
  }
}

async function markHandled(alert: AlertInfo) {
  handlingAlertId.value = alert.alert_id;
  try {
    await handleAlert(alert.alert_id);
    await loadLiveData();
  } catch (err) {
    error.value = errorMessage(err);
  } finally {
    handlingAlertId.value = "";
  }
}

watch(nodeId, () => void load(), { immediate: true });

onMounted(() => {
  refreshTimer = window.setInterval(() => void loadLiveData(), 15000);
  tickTimer = window.setInterval(() => {
    now.value = Date.now();
  }, 1000);
});

onBeforeUnmount(() => {
  if (refreshTimer) window.clearInterval(refreshTimer);
  if (tickTimer) window.clearInterval(tickTimer);
});
</script>

<template>
  <div class="node-console">
    <header class="node-hero">
      <div>
        <div class="node-title-row">
          <h1>{{ node?.id || nodeId }}</h1>
          <span class="status-pill"><i />在线</span>
          <span class="area-pill">温室A-1区</span>
        </div>
        <dl class="node-meta">
          <div><dt>节点ID：</dt><dd>{{ node?.id || nodeId }}</dd></div>
          <div><dt>节点类型：</dt><dd>智能温室节点</dd></div>
          <div><dt>安装位置：</dt><dd>温室A-1区 东侧立柱</dd></div>
          <div><dt>负责人：</dt><dd>课程管理员</dd></div>
          <div><dt>更新时间：</dt><dd>{{ updatedAt }}</dd></div>
        </dl>
      </div>

      <div class="node-actions">
        <button class="button" type="button" @click="load"><RefreshCw :size="16" />刷新</button>
        <RouterLink class="button primary" :to="`/nodes/${nodeId}/templates`">
          <SlidersHorizontal :size="16" />配置管理
        </RouterLink>
      </div>
    </header>

    <DataState :status="status" :message="error" @retry="load">
      <section class="node-metrics">
        <article v-for="card in summaryCards" :key="card.label" class="node-metric-card">
          <p>{{ card.label }}</p>
          <div class="metric-body">
            <span class="metric-icon" :class="card.tone"><component :is="card.icon" :size="30" /></span>
            <strong>{{ card.value }} <em>{{ card.suffix }}</em></strong>
          </div>
          <small>{{ card.helper }}</small>
          <div class="metric-progress"><i :style="{ width: `${card.progress}%` }" /></div>
        </article>
      </section>

      <section class="node-main-grid">
        <article class="work-panel trend-panel">
          <div class="panel-head">
            <h2>环境数据趋势 <span>最近24小时</span></h2>
            <div class="panel-tools">
              <button class="mini-select" type="button">最近24小时</button>
            </div>
          </div>
          <ChartPanel title="环境数据趋势" :points="emptyPoints" :series="trendSeries" />
        </article>

        <article class="work-panel device-panel">
          <div class="panel-head">
            <h2>设备运行统计</h2>
            <RouterLink :to="`/nodes/${nodeId}/control`">更多</RouterLink>
          </div>
          <div class="device-stat-body">
            <div class="donut" :style="{ '--running': `${runningPercent}%` }">
              <strong>{{ deviceSummary.total }}</strong>
              <span>总设备</span>
            </div>
            <ul class="device-legend">
              <li><i class="green" />运行中 <strong>{{ deviceSummary.running }} ({{ deviceRate(deviceSummary.running) }}%)</strong></li>
              <li><i class="yellow" />空闲中 <strong>{{ deviceSummary.idle }} ({{ deviceRate(deviceSummary.idle) }}%)</strong></li>
              <li><i class="teal" />离线 <strong>{{ deviceSummary.offline }} ({{ deviceRate(deviceSummary.offline) }}%)</strong></li>
              <li><i class="red" />故障 <strong>{{ deviceSummary.fault }} ({{ deviceRate(deviceSummary.fault) }}%)</strong></li>
            </ul>
          </div>
          <div class="equipment-mini-grid">
            <div v-for="item in deviceSummary.items" :key="item.key" class="equipment-mini-card">
              <span>{{ item.name }}</span>
              <strong>{{ item.total }}</strong>
              <small>{{ item.state === "running" ? `运行中 ${item.running}` : item.state === "idle" ? "空闲中 0" : "离线中 0" }}</small>
            </div>
          </div>
        </article>
      </section>

      <section class="node-bottom-grid">
        <article class="work-panel table-panel">
          <div class="panel-head">
            <h2>告警信息</h2>
            <RouterLink to="/alerts">更多</RouterLink>
          </div>
          <div class="tab-row">
            <button :class="{ active: activeAlertTab === 'all' }" type="button" @click="activeAlertTab = 'all'">全部({{ alerts?.items.length ?? 0 }})</button>
            <button :class="{ active: activeAlertTab === 'unhandled' }" type="button" @click="activeAlertTab = 'unhandled'">未处理({{ unhandledAlerts.length }})</button>
            <button :class="{ active: activeAlertTab === 'handled' }" type="button" @click="activeAlertTab = 'handled'">已处理({{ handledAlerts.length }})</button>
          </div>
          <div class="compact-table-wrap">
            <table class="compact-table">
              <thead>
                <tr>
                  <th>告警级别</th>
                  <th>告警内容</th>
                  <th>来源设备</th>
                  <th>发生时间</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="alert in filteredAlerts" :key="alert.alert_id">
                  <td><span class="warn-level"><Gauge :size="14" />{{ alert.level }}</span></td>
                  <td>{{ alert.message }}</td>
                  <td>{{ alert.source_name || alert.source }}</td>
                  <td>{{ formatDateTime(alert.timestamp) }}</td>
                  <td><span class="state-tag" :class="{ done: alert.is_handled }">{{ alert.is_handled ? "已处理" : "未处理" }}</span></td>
                  <td>
                    <button
                      v-if="!alert.is_handled"
                      class="text-action"
                      type="button"
                      :disabled="handlingAlertId === alert.alert_id"
                      @click="markHandled(alert)"
                    >
                      处理
                    </button>
                    <span v-else class="muted">-</span>
                  </td>
                </tr>
                <tr v-if="!filteredAlerts.length">
                  <td colspan="6"><div class="empty-inline">当前筛选条件下暂无告警</div></td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="work-panel table-panel">
          <div class="panel-head">
            <h2>今日任务</h2>
            <RouterLink to="/tasks">更多</RouterLink>
          </div>
          <div class="compact-table-wrap">
            <table class="compact-table">
              <thead>
                <tr>
                  <th>任务名称</th>
                  <th>任务类型</th>
                  <th>执行时间</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="task in todayTasks" :key="task.id">
                  <td>{{ task.taskName }}</td>
                  <td>{{ equipmentLabel(task.equipmentKey) }} · {{ actionLabel(task.action) }}</td>
                  <td>{{ formatTime(task.hour, task.minute) }}</td>
                  <td>
                    <span class="task-state" :class="{ pending: !task.enabled }">
                      <Wifi v-if="task.enabled" :size="14" />
                      {{ task.enabled ? `进行中 · ${formatRepeatDays(task.repeatDays)}` : "待执行" }}
                    </span>
                  </td>
                </tr>
                <tr v-if="!todayTasks.length">
                  <td colspan="4"><div class="empty-inline">暂无任务，可前往任务调度新增</div></td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <section class="sensor-strip">
        <article v-for="item in sensorCards" :key="item.key">
          <component :is="item.icon" :size="17" />
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}{{ item.unit }}</strong>
        </article>
      </section>
    </DataState>
  </div>
</template>

<style scoped>
.node-console {
  display: grid;
  gap: 18px;
}

.node-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  border-bottom: 1px solid #e5ebef;
  padding-bottom: 24px;
}

.node-title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.node-title-row h1 {
  margin: 0;
  color: #111820;
  font-size: 30px;
  line-height: 1.15;
  font-weight: 850;
}

.status-pill,
.area-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  border-radius: 4px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 850;
}

.status-pill {
  gap: 7px;
  background: #e8f8ee;
  color: #038348;
}

.status-pill i {
  width: 9px;
  height: 9px;
  border-radius: 99px;
  background: currentColor;
}

.area-pill {
  border: 1px solid #d7e2e8;
  background: #fff;
  color: #687681;
}

.node-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 26px;
  margin: 18px 0 0;
}

.node-meta div {
  display: flex;
  gap: 5px;
}

.node-meta dt,
.node-meta dd {
  margin: 0;
  color: #72808b;
  font-size: 13px;
}

.node-meta dd {
  color: #52616c;
  font-weight: 700;
}

.node-actions {
  display: flex;
  gap: 12px;
}

.node-metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 18px;
}

.node-metric-card,
.work-panel {
  border: 1px solid #e4ebef;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 6px 18px rgba(19, 40, 31, 0.04);
}

.node-metric-card {
  min-height: 144px;
  padding: 20px 22px;
}

.node-metric-card p,
.node-metric-card small {
  margin: 0;
  color: #61707b;
  font-size: 13px;
  font-weight: 760;
}

.metric-body {
  display: flex;
  align-items: center;
  gap: 22px;
  margin: 18px 0 8px;
}

.metric-icon {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 999px;
}

.metric-icon.green {
  background: linear-gradient(180deg, #16a05a, #078143);
  color: #fff;
}

.metric-icon.blue {
  background: #e7f2ff;
  color: #1884e8;
}

.metric-icon.orange {
  background: #fff4dd;
  color: #efa30b;
}

.metric-icon.teal {
  background: #e1f8f6;
  color: #16aaa2;
}

.metric-body strong {
  color: #15212b;
  font-size: 28px;
  line-height: 1;
  font-weight: 850;
}

.metric-body em {
  color: #038348;
  font-size: 13px;
  font-style: normal;
}

.metric-progress {
  height: 4px;
  margin-top: 14px;
  border-radius: 999px;
  background: #edf2f4;
  overflow: hidden;
}

.metric-progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #0a9a57, #62bd75);
}

.node-main-grid,
.node-bottom-grid {
  display: grid;
  gap: 16px;
}

.node-main-grid {
  grid-template-columns: minmax(0, 1.25fr) minmax(360px, 0.75fr);
}

.node-bottom-grid {
  grid-template-columns: minmax(0, 1.05fr) minmax(380px, 0.95fr);
}

.work-panel {
  padding: 18px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-head h2 {
  margin: 0;
  color: #17212b;
  font-size: 17px;
}

.panel-head h2 span {
  color: #8b96a0;
  font-size: 13px;
  font-weight: 650;
}

.panel-head a {
  color: #73808a;
  font-size: 13px;
  font-weight: 800;
}

.panel-tools {
  display: flex;
  gap: 8px;
}

.mini-select {
  min-height: 34px;
  border: 1px solid #dfe8ee;
  border-radius: 4px;
  background: #fff;
  color: #52616c;
  padding: 0 12px;
  font-weight: 750;
}

.trend-panel :deep(.chart) {
  min-height: 302px;
}

.device-stat-body {
  display: grid;
  grid-template-columns: 210px 1fr;
  align-items: center;
  gap: 22px;
  padding: 8px 6px 18px;
}

.donut {
  --running: 75%;
  position: relative;
  display: grid;
  place-items: center;
  width: 148px;
  height: 148px;
  border-radius: 50%;
  background: conic-gradient(#55ba63 0 var(--running), #f0b51a var(--running) 92%, #00a9a1 92% 96%, #f04438 96% 100%);
  justify-self: center;
}

.donut::after {
  content: "";
  position: absolute;
  inset: 28px;
  border-radius: inherit;
  background: #fff;
}

.donut strong,
.donut span {
  position: relative;
  z-index: 1;
}

.donut strong {
  font-size: 28px;
}

.donut span {
  margin-top: 38px;
  color: #5c6872;
  font-size: 13px;
  font-weight: 800;
}

.device-legend {
  display: grid;
  gap: 13px;
  margin: 0;
  padding: 0;
  list-style: none;
  color: #3d4a55;
  font-size: 14px;
}

.device-legend li {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
}

.device-legend i {
  width: 8px;
  height: 8px;
  border-radius: 99px;
}

.device-legend .green {
  background: #55ba63;
}

.device-legend .yellow {
  background: #f0b51a;
}

.device-legend .teal {
  background: #00a9a1;
}

.device-legend .red {
  background: #f04438;
}

.equipment-mini-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.equipment-mini-card {
  display: grid;
  place-items: center;
  gap: 5px;
  min-height: 78px;
  border: 1px solid #e1e9ee;
  border-radius: 4px;
  background: #fff;
}

.equipment-mini-card span,
.equipment-mini-card small {
  color: #4e5c67;
  font-size: 13px;
}

.equipment-mini-card strong {
  font-size: 21px;
}

.tab-row {
  display: flex;
  gap: 22px;
  border-bottom: 1px solid #e5ebef;
  margin-bottom: 8px;
}

.tab-row button {
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  padding: 0 0 10px;
  color: #697782;
  font-weight: 820;
}

.tab-row button.active {
  border-color: #078143;
  color: #078143;
}

.compact-table-wrap {
  overflow-x: auto;
}

.compact-table {
  width: 100%;
  min-width: 620px;
  border-collapse: collapse;
}

.compact-table th,
.compact-table td {
  border-bottom: 1px solid #e5ebef;
  padding: 11px 12px;
  color: #34424d;
  font-size: 13px;
  white-space: nowrap;
}

.compact-table th {
  background: #fafcfd;
  color: #5d6b76;
  font-weight: 850;
}

.warn-level,
.state-tag,
.task-state {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 26px;
  border-radius: 4px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 850;
}

.warn-level {
  color: #dd8d00;
}

.state-tag {
  background: #fff4db;
  color: #d87800;
}

.state-tag.done,
.task-state {
  background: #eaf8ef;
  color: #078143;
}

.task-state.pending {
  background: #f1f4f6;
  color: #6a7680;
}

.text-action {
  border: 0;
  background: transparent;
  color: #1677ff;
  font-weight: 850;
}

.empty-inline {
  display: grid;
  place-items: center;
  min-height: 70px;
  color: #7b8792;
}

.sensor-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.sensor-strip article {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #e4ebef;
  border-radius: 8px;
  background: #fff;
  padding: 13px 14px;
  color: #52616c;
}

.sensor-strip svg {
  color: #078143;
}

.sensor-strip strong {
  margin-left: auto;
  color: #111820;
}

@media (max-width: 1280px) {
  .node-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .node-main-grid,
  .node-bottom-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 780px) {
  .node-hero,
  .node-actions,
  .node-meta {
    display: grid;
  }

  .node-metrics,
  .sensor-strip {
    grid-template-columns: 1fr;
  }

  .device-stat-body,
  .equipment-mini-grid {
    grid-template-columns: 1fr;
  }
}
</style>
