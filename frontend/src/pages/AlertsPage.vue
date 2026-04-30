<script setup lang="ts">
import { CheckCircle2, RefreshCcw } from "lucide-vue-next";
import { onMounted, ref } from "vue";

import { handleAlert, listAlerts } from "../api/alerts";
import type { AlertInfo, AlertList } from "../api/types";
import DataState from "../components/DataState.vue";
import { defaultNodeId } from "../utils/domain";
import { errorMessage, formatDateTime } from "../utils/format";

const handledFilter = ref<"all" | "unhandled" | "handled">("unhandled");
const page = ref(1);
const size = 10;
const data = ref<AlertList | null>(null);
const status = ref<"idle" | "loading" | "ready" | "empty" | "error">("idle");
const error = ref("");
const message = ref("");

function handledValue() {
  if (handledFilter.value === "unhandled") return false;
  if (handledFilter.value === "handled") return true;
  return null;
}

async function load() {
  status.value = "loading";
  error.value = "";
  try {
    data.value = await listAlerts({ nodeId: defaultNodeId, handled: handledValue(), page: page.value, size });
    status.value = data.value.items.length ? "ready" : "empty";
  } catch (err) {
    error.value = errorMessage(err);
    status.value = "error";
  }
}

async function markHandled(alert: AlertInfo) {
  message.value = "";
  try {
    await handleAlert(alert.alert_id);
    message.value = "告警已处理";
    await load();
  } catch (err) {
    message.value = errorMessage(err);
  }
}

function changeFilter(value: typeof handledFilter.value) {
  handledFilter.value = value;
  page.value = 1;
  void load();
}

function nextPage(delta: number) {
  page.value = Math.max(1, page.value + delta);
  void load();
}

onMounted(load);
</script>

<template>
  <div class="page-stack">
    <header class="page-head">
      <div>
        <p class="eyebrow">Alerts</p>
        <h1>告警列表与处理</h1>
        <p class="muted">{{ defaultNodeId }} · 支持未处理筛选、分页和处理操作。</p>
      </div>
      <button class="button" type="button" @click="load"><RefreshCcw :size="17" />刷新</button>
    </header>

    <section class="toolbar card pad">
      <div class="segmented">
        <button :class="{ active: handledFilter === 'unhandled' }" type="button" @click="changeFilter('unhandled')">未处理</button>
        <button :class="{ active: handledFilter === 'handled' }" type="button" @click="changeFilter('handled')">已处理</button>
        <button :class="{ active: handledFilter === 'all' }" type="button" @click="changeFilter('all')">全部</button>
      </div>
      <div class="toolbar-right">
        <span class="badge warning">未处理 {{ data?.unhandled_count ?? 0 }}</span>
        <span class="badge teal">总数 {{ data?.total ?? 0 }}</span>
      </div>
    </section>

    <div v-if="message" class="message" :class="message.includes('已') ? 'success' : 'error'">{{ message }}</div>

    <DataState :status="status" :message="error || '暂无告警数据'" @retry="load">
      <section class="card pad table-wrap">
        <table>
          <thead>
            <tr>
              <th>告警</th>
              <th>来源</th>
              <th>级别</th>
              <th>时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="alert in data?.items" :key="alert.alert_id">
              <td><strong>{{ alert.type_name }}</strong><br /><span class="muted">{{ alert.message }}</span></td>
              <td>{{ alert.source_name }}<br /><span class="muted">{{ alert.node_id }}</span></td>
              <td><span class="badge" :class="alert.level === 'HIGH' ? 'danger' : 'warning'">{{ alert.level }}</span></td>
              <td>{{ formatDateTime(alert.timestamp) }}</td>
              <td><span class="badge" :class="alert.is_handled ? 'teal' : 'warning'">{{ alert.is_handled ? "已处理" : "未处理" }}</span></td>
              <td>
                <button class="button primary" type="button" :disabled="alert.is_handled" @click="markHandled(alert)">
                  <CheckCircle2 :size="16" />处理
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="toolbar">
        <button class="button" type="button" :disabled="page <= 1" @click="nextPage(-1)">上一页</button>
        <span class="badge">第 {{ page }} 页</span>
        <button class="button" type="button" :disabled="(data?.items.length || 0) < size" @click="nextPage(1)">下一页</button>
      </section>
    </DataState>
  </div>
</template>

<style scoped>
.toolbar-right {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
