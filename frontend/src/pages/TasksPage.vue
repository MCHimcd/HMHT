<script setup lang="ts">
import { Edit3, Plus, Power, Trash2 } from "lucide-vue-next";
import { computed, onMounted, reactive, ref } from "vue";

import { createTask, deleteTask, listTasks, toggleTask, updateTask } from "../api/tasks";
import type { EquipmentKey, TaskInfo, TaskRequest } from "../api/types";
import DataState from "../components/DataState.vue";
import { actionLabel, defaultNodeId, equipmentLabel, equipmentOptions } from "../utils/domain";
import { errorMessage, formatRepeatDays, formatTime } from "../utils/format";
import { validateTaskRequest } from "../utils/tasks";

const status = ref<"idle" | "loading" | "ready" | "empty" | "error">("idle");
const tasks = ref<TaskInfo[]>([]);
const error = ref("");
const message = ref("");
const modalOpen = ref(false);
const editingId = ref<number | null>(null);

const form = reactive<TaskRequest>({
  nodeId: defaultNodeId,
  equipmentKey: "water_pump",
  action: "turn_on",
  taskName: "",
  hour: 8,
  minute: 30,
  repeatDays: "1,2,3,4,5,6,7",
});

const modalTitle = computed(() => (editingId.value == null ? "创建定时任务" : "编辑定时任务"));
const dayOptions = [
  { value: "1", label: "一" },
  { value: "2", label: "二" },
  { value: "3", label: "三" },
  { value: "4", label: "四" },
  { value: "5", label: "五" },
  { value: "6", label: "六" },
  { value: "7", label: "日" },
];

function resetForm(task?: TaskInfo) {
  editingId.value = task?.id ?? null;
  form.nodeId = task?.nodeId || defaultNodeId;
  form.equipmentKey = task?.equipmentKey || "water_pump";
  form.action = task?.action || "turn_on";
  form.taskName = task?.taskName || "";
  form.hour = task?.hour ?? 8;
  form.minute = task?.minute ?? 30;
  form.repeatDays = task?.repeatDays || "1,2,3,4,5,6,7";
}

function selectedDays() {
  return new Set(form.repeatDays.split(",").filter(Boolean));
}

function toggleDay(value: string) {
  const set = selectedDays();
  if (set.has(value)) {
    set.delete(value);
  } else {
    set.add(value);
  }
  form.repeatDays = Array.from(set).sort((a, b) => Number(a) - Number(b)).join(",");
}

function validate() {
  return validateTaskRequest(form);
}

async function load() {
  status.value = "loading";
  error.value = "";
  try {
    tasks.value = await listTasks();
    status.value = tasks.value.length ? "ready" : "empty";
  } catch (err) {
    error.value = errorMessage(err);
    status.value = "error";
  }
}

function openCreate() {
  resetForm();
  modalOpen.value = true;
}

function openEdit(task: TaskInfo) {
  resetForm(task);
  modalOpen.value = true;
}

async function submit() {
  message.value = "";
  const validation = validate();
  if (validation) {
    message.value = validation;
    return;
  }
  try {
    if (editingId.value == null) {
      await createTask({ ...form });
      message.value = "任务已创建";
    } else {
      await updateTask(editingId.value, { ...form });
      message.value = "任务已更新";
    }
    modalOpen.value = false;
    await load();
  } catch (err) {
    message.value = errorMessage(err);
  }
}

async function remove(task: TaskInfo) {
  if (!window.confirm(`确认删除任务「${task.taskName}」？`)) return;
  message.value = "";
  try {
    await deleteTask(task.id);
    message.value = "任务已删除";
    await load();
  } catch (err) {
    message.value = errorMessage(err);
  }
}

async function toggle(task: TaskInfo) {
  message.value = "";
  try {
    await toggleTask(task.id);
    message.value = "任务状态已更新";
    await load();
  } catch (err) {
    message.value = errorMessage(err);
  }
}

onMounted(load);
</script>

<template>
  <div class="page-stack">
    <header class="page-head">
      <div>
        <p class="eyebrow">Scheduled Tasks</p>
        <h1>定时任务管理</h1>
        <p class="muted">任务仅保存课程配置，不向真实设备下发命令。</p>
      </div>
      <button class="button primary" type="button" @click="openCreate"><Plus :size="17" />新建任务</button>
    </header>

    <div v-if="message" class="message" :class="message.includes('已') ? 'success' : 'error'">{{ message }}</div>

    <DataState :status="status" :message="error || '暂无定时任务，可创建一个演示任务'" @retry="load">
      <section class="card pad table-wrap">
        <table>
          <thead>
            <tr>
              <th>任务</th>
              <th>设备</th>
              <th>动作</th>
              <th>时间</th>
              <th>重复</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in tasks" :key="task.id">
              <td><strong>{{ task.taskName }}</strong><br /><span class="muted">{{ task.nodeId }}</span></td>
              <td>{{ equipmentLabel(task.equipmentKey) }}</td>
              <td>{{ actionLabel(task.action) }}</td>
              <td>{{ formatTime(task.hour, task.minute) }}</td>
              <td>周{{ formatRepeatDays(task.repeatDays) }}</td>
              <td><span class="badge" :class="task.enabled ? 'teal' : 'warning'">{{ task.enabled ? "启用" : "停用" }}</span></td>
              <td class="actions">
                <button class="icon-button" type="button" title="启停" @click="toggle(task)"><Power :size="16" /></button>
                <button class="icon-button" type="button" title="编辑" @click="openEdit(task)"><Edit3 :size="16" /></button>
                <button class="icon-button" type="button" title="删除" @click="remove(task)"><Trash2 :size="16" /></button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>
    </DataState>

    <div v-if="modalOpen" class="modal-backdrop" role="dialog" aria-modal="true">
      <div class="modal">
        <div class="modal-head">
          <h2>{{ modalTitle }}</h2>
          <button class="icon-button" type="button" aria-label="关闭" @click="modalOpen = false">×</button>
        </div>
        <form class="modal-body form-grid" @submit.prevent="submit">
          <label class="field">
            <span>任务名称</span>
            <input v-model="form.taskName" maxlength="30" placeholder="例如：每日灌溉" />
          </label>
          <label class="field">
            <span>节点</span>
            <input v-model="form.nodeId" />
          </label>
          <label class="field">
            <span>设备</span>
            <select v-model="form.equipmentKey">
              <option v-for="item in equipmentOptions" :key="item.key" :value="item.key">{{ item.label }}</option>
            </select>
          </label>
          <label class="field">
            <span>动作</span>
            <select v-model="form.action">
              <option value="turn_on">开启</option>
              <option value="turn_off">关闭</option>
            </select>
          </label>
          <label class="field">
            <span>小时</span>
            <input v-model.number="form.hour" type="number" min="0" max="23" />
          </label>
          <label class="field">
            <span>分钟</span>
            <input v-model.number="form.minute" type="number" min="0" max="59" />
          </label>
          <div class="field days">
            <span>重复日期</span>
            <div class="day-row">
              <button
                v-for="day in dayOptions"
                :key="day.value"
                class="day"
                :class="{ active: selectedDays().has(day.value) }"
                type="button"
                @click="toggleDay(day.value)"
              >
                {{ day.label }}
              </button>
            </div>
          </div>
          <div class="modal-actions">
            <button class="button" type="button" @click="modalOpen = false">取消</button>
            <button class="button primary" type="submit">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.actions {
  display: flex;
  gap: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.days,
.modal-actions {
  grid-column: 1 / -1;
}

.day-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.day {
  min-width: 38px;
  min-height: 34px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  font-weight: 800;
}

.day.active {
  border-color: var(--green);
  background: var(--green);
  color: #fff;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
