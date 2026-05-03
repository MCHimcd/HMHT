<script setup lang="ts">
import { Activity, Droplets, RefreshCcw, SunMedium, Thermometer, Waves } from "lucide-vue-next";
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";

import { getSensorHistory } from "../api/sensors";
import type { SensorHistory, SensorMetric } from "../api/types";
import ChartPanel from "../components/ChartPanel.vue";
import DataState from "../components/DataState.vue";
import { formatMetricValue, latestMetricValue } from "../utils/console";
import { sensorOptions } from "../utils/domain";
import { errorMessage } from "../utils/format";

const route = useRoute();
const nodeId = computed(() => String(route.params.nodeId));
const status = ref<"idle" | "loading" | "ready" | "empty" | "error">("idle");
const error = ref("");
const histories = ref<Partial<Record<SensorMetric, SensorHistory>>>({});
const updatedAt = ref(Date.now());
let refreshTimer: number | undefined;

const iconMap = {
  temperature: Thermometer,
  humidity: Droplets,
  light: SunMedium,
  soilHumidity: Waves,
} as const;

const cards = computed(() =>
  sensorOptions.map((item) => ({
    ...item,
    icon: iconMap[item.key],
    value: formatMetricValue(latestMetricValue(histories.value, item.key), item.key),
    summary: histories.value[item.key]?.summary,
  })),
);

const trendSeries = computed(() =>
  sensorOptions
    .map((item) => ({
      name: `${item.label}${item.unit ? `(${item.unit})` : ""}`,
      unit: item.unit,
      color: item.key === "temperature" ? "#54b948" : item.key === "humidity" ? "#278af0" : item.key === "light" ? "#f5a400" : "#22b8b8",
      axis: item.key === "light" ? ("secondary" as const) : ("primary" as const),
      points: histories.value[item.key]?.points ?? [],
    }))
    .filter((item) => item.points.length),
);

const emptyPoints = computed(() => trendSeries.value[0]?.points ?? []);

async function load() {
  status.value = "loading";
  error.value = "";
  try {
    const entries = await Promise.all(
      sensorOptions.map(async (item) => [item.key, await getSensorHistory(nodeId.value, item.key, "24h")] as const),
    );
    histories.value = Object.fromEntries(entries) as Partial<Record<SensorMetric, SensorHistory>>;
    updatedAt.value = Date.now();
    status.value = entries.some(([, value]) => value.points.length) ? "ready" : "empty";
  } catch (err) {
    error.value = errorMessage(err);
    status.value = "error";
  }
}

watch(nodeId, () => void load(), { immediate: true });

onMounted(() => {
  refreshTimer = window.setInterval(() => void load(), 2000);
});

onBeforeUnmount(() => {
  if (refreshTimer) window.clearInterval(refreshTimer);
});
</script>

<template>
  <div class="page-stack">
    <header class="page-head">
      <div>
        <p class="eyebrow">Realtime</p>
        <h1>实时数据</h1>
        <p class="muted">{{ nodeId }} · 每 2 秒自动刷新一次，适合查看当前环境波动。</p>
      </div>
      <button class="button" type="button" @click="load"><RefreshCcw :size="17" />刷新</button>
    </header>

    <DataState :status="status" :message="error || '暂无实时数据'" @retry="load">
      <section class="grid four">
        <article v-for="card in cards" :key="card.key" class="metric card pad">
          <div class="metric-head">
            <component :is="card.icon" :size="18" />
            <span>{{ card.label }}</span>
          </div>
          <strong>{{ card.value }}<em>{{ card.unit }}</em></strong>
          <small>均值 {{ card.summary?.avg.toFixed(1) ?? "-" }}{{ card.unit }}</small>
        </article>
      </section>

      <section class="card pad">
        <div class="toolbar">
          <div>
            <h2>最近 24 小时环境趋势</h2>
            <p class="muted">已按光照单独使用右轴，避免其余曲线被压扁。</p>
          </div>
          <span class="badge teal"><Activity :size="14" />{{ new Date(updatedAt).toLocaleTimeString("zh-CN", { hour12: false }) }}</span>
        </div>
        <ChartPanel title="实时趋势" :points="emptyPoints" :series="trendSeries" />
      </section>
    </DataState>
  </div>
</template>

<style scoped>
.metric {
  display: grid;
  gap: 10px;
}

.metric-head {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-weight: 800;
}

.metric-head svg {
  color: var(--green);
}

.metric strong {
  font-size: 32px;
  line-height: 1;
}

.metric em {
  margin-left: 6px;
  color: var(--green);
  font-size: 14px;
  font-style: normal;
}

.metric small {
  color: var(--muted);
  font-weight: 700;
}
</style>
