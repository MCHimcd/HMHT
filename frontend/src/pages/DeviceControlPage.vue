<script setup lang="ts">
import { Droplets, LampDesk, Power, RefreshCcw, Wind } from "lucide-vue-next";
import { computed, onMounted, reactive, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";

import { getEquipmentMeta } from "../api/nodes";
import type { EquipmentKey, EquipmentMeta } from "../api/types";
import DataState from "../components/DataState.vue";
import { equipmentOptions } from "../utils/domain";
import { errorMessage } from "../utils/format";

const route = useRoute();
const nodeId = computed(() => String(route.params.nodeId));
const equipment = ref<EquipmentMeta[]>([]);
const status = ref<"idle" | "loading" | "ready" | "empty" | "error">("idle");
const error = ref("");
const message = ref("");

const controlState = reactive<Record<EquipmentKey, number>>({
  water_pump: 1,
  led_light: 65,
  fan: 1,
});

const iconMap = {
  water_pump: Droplets,
  led_light: LampDesk,
  fan: Wind,
} as const;

const presets = [
  { label: "通风优先", values: { water_pump: 0, led_light: 45, fan: 1 } },
  { label: "灌溉优先", values: { water_pump: 1, led_light: 55, fan: 0 } },
  { label: "夜间模式", values: { water_pump: 0, led_light: 25, fan: 0 } },
];

const fallbackEquipment: EquipmentMeta[] = equipmentOptions.map((item) => ({
  key: item.key,
  name: item.label,
  icon: item.key,
  controllable: true,
  controlType: item.key === "led_light" ? "slider" : "toggle",
}));

const controlCards = computed(() =>
  (equipment.value.length ? equipment.value : fallbackEquipment).map((item) => ({
    ...item,
    icon: iconMap[item.key],
    value: controlState[item.key],
  })),
);

async function load() {
  status.value = "loading";
  error.value = "";
  try {
    equipment.value = await getEquipmentMeta(nodeId.value);
    status.value = "ready";
  } catch (err) {
    error.value = errorMessage(err);
    status.value = "error";
  }
}

function applyPreset(values: Partial<Record<EquipmentKey, number>>, label: string) {
  for (const [key, value] of Object.entries(values)) {
    controlState[key as EquipmentKey] = value ?? 0;
  }
  message.value = `已应用${label}`;
}

function toggleDevice(key: EquipmentKey) {
  controlState[key] = controlState[key] > 0 ? 0 : 1;
  message.value = "控制状态已更新";
}

watch(nodeId, () => void load(), { immediate: true });
</script>

<template>
  <div class="page-stack">
    <header class="page-head">
      <div>
        <p class="eyebrow">Control</p>
        <h1>设备控制</h1>
        <p class="muted">{{ nodeId }} · 课程版只做前端控制面板演示，不向真实硬件下发命令。</p>
      </div>
      <button class="button" type="button" @click="load"><RefreshCcw :size="17" />刷新</button>
    </header>

    <section class="toolbar card pad">
      <div class="segmented">
        <button v-for="preset in presets" :key="preset.label" type="button" @click="applyPreset(preset.values, preset.label)">{{ preset.label }}</button>
      </div>
      <RouterLink class="button primary" to="/tasks">进入任务调度</RouterLink>
    </section>

    <div v-if="message" class="message success">{{ message }}</div>

    <DataState :status="status" :message="error || '暂无设备控制面板'" @retry="load">
      <section class="grid three">
        <article v-for="item in controlCards" :key="item.key" class="card pad control-card">
          <div class="control-head">
            <div class="control-icon"><component :is="item.icon" :size="22" /></div>
            <div>
              <h2>{{ item.name }}</h2>
              <p class="muted">{{ item.controlType === 'slider' ? '连续调节' : '开关控制' }}</p>
            </div>
          </div>

          <template v-if="item.controlType === 'slider'">
            <input v-model.number="controlState[item.key]" type="range" min="0" max="100" step="5" />
            <div class="control-row">
              <span>当前强度</span>
              <strong>{{ controlState[item.key] }}%</strong>
            </div>
          </template>
          <template v-else>
            <button class="toggle" :class="{ active: controlState[item.key] > 0 }" type="button" @click="toggleDevice(item.key)">
              <Power :size="16" />
              {{ controlState[item.key] > 0 ? "运行中" : "已关闭" }}
            </button>
            <div class="control-row">
              <span>联动方式</span>
              <strong>{{ controlState[item.key] > 0 ? "手动接管" : "等待策略" }}</strong>
            </div>
          </template>
        </article>
      </section>
    </DataState>
  </div>
</template>

<style scoped>
.control-card {
  display: grid;
  gap: 18px;
}

.control-head {
  display: flex;
  align-items: center;
  gap: 14px;
}

.control-head h2,
.control-head p {
  margin: 0;
}

.control-icon {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: var(--green-soft);
  color: var(--green);
}

input[type="range"] {
  width: 100%;
  accent-color: var(--green);
}

.toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 42px;
  border: 1px solid var(--border-strong);
  border-radius: 10px;
  background: #fff;
  color: var(--muted-strong);
  font-weight: 800;
}

.toggle.active {
  border-color: var(--green);
  background: var(--green);
  color: #fff;
}

.control-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--muted);
  font-weight: 700;
}

.control-row strong {
  color: var(--text);
}
</style>
