<script setup lang="ts">
import { RefreshCcw } from "lucide-vue-next";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";

import { getSensorHistory } from "../api/sensors";
import type { SensorHistory, SensorMetric, SensorRange } from "../api/types";
import ChartPanel from "../components/ChartPanel.vue";
import DataState from "../components/DataState.vue";
import { rangeOptions, sensorLabel, sensorOptions, sensorUnit } from "../utils/domain";
import { errorMessage } from "../utils/format";

const route = useRoute();
const nodeId = computed(() => String(route.params.nodeId));
const metric = ref<SensorMetric>("temperature");
const range = ref<SensorRange>("24h");
const data = ref<SensorHistory | null>(null);
const status = ref<"idle" | "loading" | "ready" | "empty" | "error">("idle");
const error = ref("");

async function load() {
  status.value = "loading";
  error.value = "";
  try {
    data.value = await getSensorHistory(nodeId.value, metric.value, range.value);
    status.value = data.value.points.length ? "ready" : "empty";
  } catch (err) {
    error.value = errorMessage(err);
    status.value = "error";
  }
}

watch([metric, range, nodeId], load);
onMounted(load);
</script>

<template>
  <div class="page-stack">
    <header class="page-head">
      <div>
        <p class="eyebrow">Sensor History</p>
        <h1>{{ sensorLabel(metric) }}曲线</h1>
        <p class="muted">{{ nodeId }} · 切换指标或时间范围后自动重新加载。</p>
      </div>
      <button class="button" type="button" @click="load"><RefreshCcw :size="17" />刷新</button>
    </header>

    <section class="toolbar card pad">
      <div class="segmented">
        <button v-for="item in sensorOptions" :key="item.key" :class="{ active: metric === item.key }" type="button" @click="metric = item.key">
          {{ item.label }}
        </button>
      </div>
      <div class="segmented">
        <button v-for="item in rangeOptions" :key="item.key" :class="{ active: range === item.key }" type="button" @click="range = item.key">
          {{ item.label }}
        </button>
      </div>
    </section>

    <DataState :status="status" :message="error || '暂无曲线点位'" @retry="load">
      <section class="grid three">
        <article class="metric card pad"><span class="muted">最大值</span><strong>{{ data?.summary.max.toFixed(1) }}{{ sensorUnit(metric) }}</strong></article>
        <article class="metric card pad"><span class="muted">最小值</span><strong>{{ data?.summary.min.toFixed(1) }}{{ sensorUnit(metric) }}</strong></article>
        <article class="metric card pad"><span class="muted">平均值</span><strong>{{ data?.summary.avg.toFixed(1) }}{{ sensorUnit(metric) }}</strong></article>
      </section>
      <section class="card pad">
        <div class="toolbar">
          <h2>{{ sensorLabel(metric) }}趋势</h2>
          <span class="badge teal">{{ range }}</span>
        </div>
        <ChartPanel :title="sensorLabel(metric)" :unit="sensorUnit(metric)" :points="data?.points || []" color="#168043" />
      </section>
    </DataState>
  </div>
</template>

<style scoped>
.metric {
  display: grid;
  gap: 8px;
}

.metric strong {
  font-size: 26px;
}

@media (max-width: 720px) {
  .segmented {
    width: 100%;
    overflow-x: auto;
  }
}
</style>
