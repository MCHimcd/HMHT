<script setup lang="ts">
import { RefreshCcw } from "lucide-vue-next";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";

import { getEquipmentStats } from "../api/sensors";
import type { EquipmentKey, EquipmentStats } from "../api/types";
import ChartPanel from "../components/ChartPanel.vue";
import DataState from "../components/DataState.vue";
import { equipmentLabel, equipmentOptions } from "../utils/domain";
import { errorMessage } from "../utils/format";

const route = useRoute();
const nodeId = computed(() => String(route.params.nodeId));
const equipmentKey = ref<EquipmentKey>("water_pump");
const data = ref<EquipmentStats | null>(null);
const status = ref<"idle" | "loading" | "ready" | "empty" | "error">("idle");
const error = ref("");

const summaryEntries = computed(() => Object.entries(data.value?.summary || {}).slice(0, 4));

async function load() {
  status.value = "loading";
  error.value = "";
  try {
    data.value = await getEquipmentStats(nodeId.value, equipmentKey.value);
    status.value = data.value.points.length ? "ready" : "empty";
  } catch (err) {
    error.value = errorMessage(err);
    status.value = "error";
  }
}

watch([equipmentKey, nodeId], load);
onMounted(load);
</script>

<template>
  <div class="page-stack">
    <header class="page-head">
      <div>
        <p class="eyebrow">Equipment Stats</p>
        <h1>{{ equipmentLabel(equipmentKey) }}统计</h1>
        <p class="muted">课程版只展示模拟统计，不做真实设备控制或命令下发。</p>
      </div>
      <button class="button" type="button" @click="load"><RefreshCcw :size="17" />刷新</button>
    </header>

    <section class="toolbar card pad">
      <div class="segmented">
        <button v-for="item in equipmentOptions" :key="item.key" :class="{ active: equipmentKey === item.key }" type="button" @click="equipmentKey = item.key">
          {{ item.label }}
        </button>
      </div>
      <span class="badge teal">{{ nodeId }}</span>
    </section>

    <DataState :status="status" :message="error || '暂无设备统计'" @retry="load">
      <section class="grid four">
        <article v-for="[key, value] in summaryEntries" :key="key" class="metric card pad">
          <span class="muted">{{ key }}</span>
          <strong>{{ value }}</strong>
        </article>
      </section>
      <section class="card pad">
        <div class="toolbar">
          <h2>{{ equipmentLabel(equipmentKey) }}运行趋势</h2>
          <span class="badge warning">模拟统计</span>
        </div>
        <ChartPanel :title="equipmentLabel(equipmentKey)" :points="data?.points || []" color="#0d8b8b" type="bar" />
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
  font-size: 24px;
}
</style>
