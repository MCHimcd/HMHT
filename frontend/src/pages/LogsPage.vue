<script setup lang="ts">
import { RefreshCcw, Search } from "lucide-vue-next";
import { onMounted, reactive, ref } from "vue";

import { listLogs } from "../api/admin";
import type { LogPage } from "../api/types";
import DataState from "../components/DataState.vue";
import { errorMessage, formatDateTime } from "../utils/format";

const status = ref<"idle" | "loading" | "ready" | "empty" | "error">("idle");
const error = ref("");
const data = ref<LogPage | null>(null);
const filters = reactive({
  page: 1,
  size: 20,
  type: "",
  targetId: "",
  startDate: "",
  endDate: "",
});

async function load() {
  status.value = "loading";
  error.value = "";
  try {
    data.value = await listLogs({ ...filters });
    status.value = data.value.items.length ? "ready" : "empty";
  } catch (err) {
    error.value = errorMessage(err);
    status.value = "error";
  }
}

function search() {
  filters.page = 1;
  void load();
}

function nextPage(delta: number) {
  filters.page = Math.max(1, filters.page + delta);
  void load();
}

onMounted(load);
</script>

<template>
  <div class="page-stack">
    <header class="page-head">
      <div>
        <p class="eyebrow">Operation Logs</p>
        <h1>操作日志</h1>
        <p class="muted">支持按类型、目标标识和日期区间筛选审计记录。</p>
      </div>
      <button class="button" type="button" @click="load"><RefreshCcw :size="17" />刷新</button>
    </header>

    <form class="card pad filter-grid" @submit.prevent="search">
      <label class="field">
        <span>操作类型</span>
        <input v-model="filters.type" placeholder="LOGIN / TASK / NODE" />
      </label>
      <label class="field">
        <span>目标 ID</span>
        <input v-model="filters.targetId" placeholder="SIM_Node_01" />
      </label>
      <label class="field">
        <span>开始日期</span>
        <input v-model="filters.startDate" type="date" />
      </label>
      <label class="field">
        <span>结束日期</span>
        <input v-model="filters.endDate" type="date" />
      </label>
      <button class="button primary" type="submit"><Search :size="17" />查询</button>
    </form>

    <DataState :status="status" :message="error || '暂无操作日志'" @retry="load">
      <section class="card pad table-wrap">
        <table>
          <thead>
            <tr>
              <th>时间</th>
              <th>用户</th>
              <th>类型</th>
              <th>目标</th>
              <th>详情</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in data?.items" :key="item.id">
              <td>{{ formatDateTime(item.createdAt) }}</td>
              <td>{{ item.userName || "-" }}<br /><span class="muted">{{ item.userId ?? "-" }}</span></td>
              <td><span class="badge teal">{{ item.operationType }}</span></td>
              <td>{{ item.targetType || "-" }}<br /><span class="muted">{{ item.targetId || "-" }}</span></td>
              <td>{{ item.detail || "-" }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="toolbar">
        <button class="button" type="button" :disabled="filters.page <= 1" @click="nextPage(-1)">上一页</button>
        <span class="badge">第 {{ filters.page }} 页 / 共 {{ data?.total ?? 0 }} 条</span>
        <button class="button" type="button" :disabled="(data?.items.length || 0) < filters.size" @click="nextPage(1)">下一页</button>
      </section>
    </DataState>
  </div>
</template>

<style scoped>
.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) auto;
  align-items: end;
  gap: 12px;
}

@media (max-width: 1100px) {
  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
