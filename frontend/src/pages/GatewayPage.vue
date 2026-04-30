<script setup lang="ts">
import { RefreshCcw, Router, Server, ShieldCheck, Sprout, Wifi } from "lucide-vue-next";
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
const alerts = ref<AlertList | null>(null);

const summaryCards = computed(() => [
  { label: "网关在线状态", value: health.value?.available ? "在线" : "离线", icon: Router },
  { label: "接入节点", value: String(nodes.value.length || 1), icon: Server },
  { label: "未处理告警", value: String(alerts.value?.unhandled_count ?? 0), icon: ShieldCheck },
  { label: "数据模式", value: health.value?.dataSimulated ? "课程模拟" : "实时接入", icon: Wifi },
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
        <p class="eyebrow">Gateway</p>
        <h1>网关管理</h1>
        <p class="muted">集中查看课程网关连通性、节点挂载状态和最近同步情况。</p>
      </div>
      <button class="button" type="button" @click="load"><RefreshCcw :size="17" />刷新</button>
    </header>

    <DataState :status="status" :message="error || '暂无网关数据'" @retry="load">
      <section class="grid four">
        <article v-for="card in summaryCards" :key="card.label" class="metric card pad">
          <component :is="card.icon" :size="20" />
          <span class="muted">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </article>
      </section>

      <section class="grid two">
        <article class="card pad">
          <div class="toolbar">
            <div>
              <h2>网关状态</h2>
              <p class="muted">课程版网关统一映射到后端服务状态和固定节点集合。</p>
            </div>
            <span class="badge teal">{{ formatDateTime(health?.time) }}</span>
          </div>
          <div class="gateway-state">
            <div class="gateway-core">
              <div class="gateway-icon"><Router :size="28" /></div>
              <div>
                <strong>HMHT-GW-01</strong>
                <p>课程模拟网关 · 节点统一接入 API 服务</p>
              </div>
            </div>
            <dl class="gateway-meta">
              <div><dt>服务版本</dt><dd>{{ health?.edition || "course" }}</dd></div>
              <div><dt>节点默认路由</dt><dd>{{ health?.nodeId || defaultNodeId }}</dd></div>
              <div><dt>数据来源</dt><dd>{{ health?.dataSimulated ? "模拟数据" : "真实数据" }}</dd></div>
              <div><dt>应用名</dt><dd>{{ health?.application || "hmht" }}</dd></div>
            </dl>
          </div>
        </article>

        <article class="card pad">
          <div class="toolbar">
            <h2>接入节点</h2>
            <span class="badge">{{ nodes.length || 1 }} 个节点</span>
          </div>
          <div class="node-list">
            <RouterLink v-for="node in nodes" :key="node.id" class="node-item" :to="`/nodes/${node.id}`">
              <span><Sprout :size="18" />{{ node.name }}</span>
              <strong>{{ node.id }}</strong>
              <em>{{ node.status }}</em>
            </RouterLink>
          </div>
        </article>
      </section>
    </DataState>
  </div>
</template>

<style scoped>
.metric {
  display: grid;
  gap: 8px;
}

.metric svg {
  color: var(--green);
}

.metric strong {
  font-size: 24px;
}

.gateway-state {
  display: grid;
  gap: 16px;
}

.gateway-core {
  display: flex;
  align-items: center;
  gap: 14px;
}

.gateway-icon {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  border-radius: 14px;
  background: var(--green-soft);
  color: var(--green);
}

.gateway-core strong {
  display: block;
  margin-bottom: 6px;
  font-size: 20px;
}

.gateway-core p {
  margin: 0;
  color: var(--muted);
}

.gateway-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 0;
}

.gateway-meta div {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 14px;
  background: var(--surface-soft);
}

.gateway-meta dt,
.gateway-meta dd {
  margin: 0;
}

.gateway-meta dt {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.gateway-meta dd {
  margin-top: 6px;
  font-weight: 800;
}

.node-list {
  display: grid;
  gap: 10px;
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

@media (max-width: 720px) {
  .gateway-meta,
  .node-item {
    grid-template-columns: 1fr;
  }
}
</style>
