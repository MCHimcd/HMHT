<script setup lang="ts">
import { CircuitBoard, RefreshCcw } from "lucide-vue-next";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";

import { getEquipmentMeta } from "../api/nodes";
import type { EquipmentMeta } from "../api/types";
import DataState from "../components/DataState.vue";
import { errorMessage } from "../utils/format";

const route = useRoute();
const nodeId = computed(() => String(route.params.nodeId));
const equipment = ref<EquipmentMeta[]>([]);
const status = ref<"idle" | "loading" | "ready" | "empty" | "error">("idle");
const error = ref("");

async function load() {
  status.value = "loading";
  error.value = "";
  try {
    equipment.value = await getEquipmentMeta(nodeId.value);
    status.value = equipment.value.length ? "ready" : "empty";
  } catch (err) {
    error.value = errorMessage(err);
    status.value = "error";
  }
}

watch(nodeId, load);
onMounted(load);
</script>

<template>
  <div class="page-stack">
    <header class="page-head">
      <div>
        <p class="eyebrow">Equipment Template</p>
        <h1>设备模板</h1>
        <p class="muted">{{ nodeId }} · 查看课程版设备类型、控制形态和可控能力。</p>
      </div>
      <button class="button" type="button" @click="load"><RefreshCcw :size="17" />刷新</button>
    </header>

    <DataState :status="status" :message="error || '暂无设备模板'" @retry="load">
      <section class="grid three">
        <article v-for="item in equipment" :key="item.key" class="template-card card pad">
          <CircuitBoard :size="28" />
          <h2>{{ item.name }}</h2>
          <p>{{ item.key }}</p>
          <span class="badge teal">{{ item.controlType }}</span>
          <span class="badge" :class="item.controllable ? 'teal' : 'warning'">{{ item.controllable ? "可控" : "只读" }}</span>
        </article>
      </section>
    </DataState>
  </div>
</template>

<style scoped>
.template-card {
  display: grid;
  gap: 10px;
}

.template-card svg {
  color: var(--green);
}

.template-card p {
  color: var(--muted);
}
</style>
