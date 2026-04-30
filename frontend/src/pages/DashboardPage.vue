<script setup lang="ts">
import { Activity, Bell, ChartLine, RefreshCcw, Server, Sprout } from "lucide-vue-next";
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import { listAlerts } from "../api/alerts";
import { getHealth } from "../api/health";
import { listNodes } from "../api/nodes";
import type { AlertList, HealthData, NodeInfo } from "../api/types";
import DataState from "../components/DataState.vue";
import { defaultNodeId } from "../utils/domain";
import { errorMessage, formatDateTime } from "../utils/format";

const status = ref<"idle" | "loading" | "ready" | "empty" | "error">("idle");
const error = ref("");
const nodes = ref<NodeInfo[]>([]);
const health = ref<HealthData | null>(null);
const alertSummary = ref<AlertList | null>(null);

const defaultNode = computed(() => nodes.value[0] || { id: defaultNodeId, name: "课程模拟温室节点", status: "online" });
const summaryCards = computed(() => [
  { label: "后端状态", value: health.value?.available ? "可用" : "未知", accent: "teal" },
  { label: "课程节点", value: defaultNode.value.id, accent: "" },
  { label: "未处理告警", value: String(alertSummary.value?.unhandled_count ?? 0), accent: "warning" },
  { label: "数据来源", value: health.value?.dataSimulated ? "模拟数据" : "真实数据", accent: "" },
]);

async function load() {
  status.value = "loading";
  error.value = "";
  try {
    const [healthResult, nodeResult, alertResult] = await Promise.all([
      getHealth(),
      listNodes(),
      listAlerts({ nodeId: defaultNodeId, handled: false, page: 1, size: 5 }),
    ]);
    health.value = healthResult;
    nodes.value = nodeResult;
    alertSummary.value = alertResult;
    status.value = nodeResult.length ? "ready" : "empty";
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
        <p class="eyebrow">Home</p>
        <h1>首页总览</h1>
        <p class="muted">以固定模拟节点 {{ defaultNodeId }} 为主线，展示控制台总览和常用入口。</p>
      </div>
      <button class="button" type="button" @click="load">
        <RefreshCcw :size="17" />刷新
      </button>
    </header>

    <DataState :status="status" :message="error || '当前账号暂无授权节点'" @retry="load">
      <section class="grid four">
        <article v-for="card in summaryCards" :key="card.label" class="metric card pad">
          <span class="muted">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <span class="badge" :class="card.accent">课程版</span>
        </article>
      </section>

      <section class="grid two">
        <article class="card pad node-panel">
          <div class="toolbar">
            <div>
              <p class="eyebrow">Node</p>
              <h2>{{ defaultNode.name }}</h2>
              <p class="muted">{{ defaultNode.id }} · {{ defaultNode.status }}</p>
            </div>
            <Sprout :size="34" />
          </div>
          <div class="action-grid">
            <RouterLink class="button primary" :to="`/nodes/${defaultNode.id}`"><Server :size="17" />节点管理</RouterLink>
            <RouterLink class="button" :to="`/nodes/${defaultNode.id}/realtime`"><ChartLine :size="17" />实时数据</RouterLink>
            <RouterLink class="button" :to="`/nodes/${defaultNode.id}/control`"><Activity :size="17" />设备控制</RouterLink>
            <RouterLink class="button warning" to="/alerts"><Bell :size="17" />告警处理</RouterLink>
          </div>
        </article>

        <article class="card pad">
          <div class="toolbar">
            <h2>最近告警</h2>
            <RouterLink class="button" to="/alerts">查看全部</RouterLink>
          </div>
          <div v-if="!alertSummary?.items.length" class="empty-state">当前没有未处理告警</div>
          <ul v-else class="alert-list">
            <li v-for="alert in alertSummary.items" :key="alert.alert_id">
              <span class="badge warning">{{ alert.level }}</span>
              <strong>{{ alert.type_name }}</strong>
              <small>{{ formatDateTime(alert.timestamp) }}</small>
              <p>{{ alert.message }}</p>
            </li>
          </ul>
        </article>
      </section>

      <section class="card pad">
        <div class="toolbar">
          <div>
            <h2>授权节点</h2>
            <p class="muted">课程版通常只有一个固定模拟节点，仍按节点列表渲染以兼容管理演示。</p>
          </div>
          <span class="badge teal">服务时间 {{ formatDateTime(health?.time) }}</span>
        </div>
        <div class="node-list">
          <RouterLink v-for="node in nodes" :key="node.id" class="node-item" :to="`/nodes/${node.id}`">
            <span><Sprout :size="18" />{{ node.name }}</span>
            <strong>{{ node.id }}</strong>
            <em>{{ node.status }}</em>
          </RouterLink>
        </div>
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

.node-panel svg {
  color: var(--green);
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 24px;
}

.alert-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.alert-list li {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 8px 10px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 10px;
}

.alert-list p {
  grid-column: 2 / -1;
  margin: 0;
  color: var(--muted);
}

.node-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.node-item {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
  background: var(--surface);
}

.node-item span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
}

.node-item em {
  color: var(--green);
  font-style: normal;
  font-weight: 800;
}

@media (max-width: 680px) {
  .action-grid,
  .alert-list li,
  .node-item {
    grid-template-columns: 1fr;
  }

  .alert-list p {
    grid-column: auto;
  }
}
</style>
