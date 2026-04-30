<script setup lang="ts">
import { RefreshCcw, Save, Trash2, UserCog } from "lucide-vue-next";
import { onMounted, reactive, ref } from "vue";

import * as adminApi from "../api/admin";
import { listNodes } from "../api/nodes";
import type { AdminUser, NodeInfo } from "../api/types";
import DataState from "../components/DataState.vue";
import { errorMessage } from "../utils/format";

const status = ref<"idle" | "loading" | "ready" | "empty" | "error">("idle");
const error = ref("");
const message = ref("");
const nodes = ref<NodeInfo[]>([]);
const users = ref<AdminUser[]>([]);
const selectedUserId = ref<number | null>(null);
const selectedUserNodes = ref<string[]>([]);

const nodeForm = reactive({
  nodeId: "",
  nodeName: "",
});

async function load() {
  status.value = "loading";
  error.value = "";
  try {
    const [nodeResult, userResult] = await Promise.all([listNodes(), adminApi.listUsers()]);
    nodes.value = nodeResult;
    users.value = userResult;
    status.value = "ready";
  } catch (err) {
    error.value = errorMessage(err);
    status.value = "error";
  }
}

async function createNode() {
  message.value = "";
  if (!nodeForm.nodeId || !nodeForm.nodeName) {
    message.value = "节点 ID 和名称不能为空";
    return;
  }
  try {
    await adminApi.createNode(nodeForm.nodeId, nodeForm.nodeName);
    message.value = "节点已创建";
    nodeForm.nodeId = "";
    nodeForm.nodeName = "";
    await load();
  } catch (err) {
    message.value = errorMessage(err);
  }
}

async function renameNode(node: NodeInfo) {
  const nodeName = window.prompt("请输入新的节点名称", node.name);
  if (!nodeName) return;
  try {
    await adminApi.updateNode(node.id, nodeName);
    message.value = "节点已更新";
    await load();
  } catch (err) {
    message.value = errorMessage(err);
  }
}

async function removeNode(node: NodeInfo) {
  if (!window.confirm(`确认删除节点「${node.name}」？`)) return;
  try {
    await adminApi.deleteNode(node.id);
    message.value = "节点已删除";
    await load();
  } catch (err) {
    message.value = errorMessage(err);
  }
}

async function changeRole(user: AdminUser, role: string) {
  try {
    await adminApi.updateUserRole(user.id, role);
    message.value = "用户角色已更新";
    await load();
  } catch (err) {
    message.value = errorMessage(err);
  }
}

function roleFromEvent(event: Event) {
  return (event.target as HTMLSelectElement).value;
}

async function selectUser(user: AdminUser) {
  selectedUserId.value = user.id;
  selectedUserNodes.value = await adminApi.getUserNodes(user.id);
}

function toggleNodeAccess(nodeId: string) {
  if (selectedUserNodes.value.includes(nodeId)) {
    selectedUserNodes.value = selectedUserNodes.value.filter((id) => id !== nodeId);
  } else {
    selectedUserNodes.value = [...selectedUserNodes.value, nodeId];
  }
}

async function saveUserNodes() {
  if (selectedUserId.value == null) return;
  try {
    await adminApi.setUserNodes(selectedUserId.value, selectedUserNodes.value);
    message.value = "用户节点授权已保存";
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
        <p class="eyebrow">Admin</p>
        <h1>管理员入口</h1>
        <p class="muted">节点、用户角色和节点授权管理，用于课程 CRUD 演示。</p>
      </div>
      <button class="button" type="button" @click="load"><RefreshCcw :size="17" />刷新</button>
    </header>

    <div v-if="message" class="message" :class="message.includes('已') ? 'success' : 'error'">{{ message }}</div>

    <DataState :status="status" :message="error" @retry="load">
      <section class="grid two">
        <article class="card pad">
          <h2>节点管理</h2>
          <form class="inline-form" @submit.prevent="createNode">
            <label class="field">
              <span>节点 ID</span>
              <input v-model="nodeForm.nodeId" placeholder="SIM_Node_02" />
            </label>
            <label class="field">
              <span>节点名称</span>
              <input v-model="nodeForm.nodeName" placeholder="课程扩展节点" />
            </label>
            <button class="button primary" type="submit">创建</button>
          </form>
          <div class="list">
            <div v-for="node in nodes" :key="node.id" class="list-row">
              <div>
                <strong>{{ node.name }}</strong>
                <p class="muted">{{ node.id }} · {{ node.status }}</p>
              </div>
              <button class="button" type="button" @click="renameNode(node)"><Save :size="16" />改名</button>
              <button class="icon-button" type="button" title="删除" @click="removeNode(node)"><Trash2 :size="16" /></button>
            </div>
          </div>
        </article>

        <article class="card pad">
          <h2>用户与授权</h2>
          <div class="list">
            <div v-for="user in users" :key="user.id" class="user-row" :class="{ active: selectedUserId === user.id }">
              <button class="user-main" type="button" @click="selectUser(user)">
                <UserCog :size="18" />
                <span><strong>{{ user.userName }}</strong><small>{{ user.phone }}</small></span>
              </button>
              <select :value="user.role" @change="changeRole(user, roleFromEvent($event))">
                <option value="user">user</option>
                <option value="admin">admin</option>
              </select>
            </div>
          </div>
          <div class="auth-box">
            <h3>节点授权</h3>
            <p class="muted">先选择用户，再勾选可访问节点。</p>
            <div class="node-checks">
              <label v-for="node in nodes" :key="node.id" class="check-row">
                <input :checked="selectedUserNodes.includes(node.id)" type="checkbox" :disabled="selectedUserId == null" @change="toggleNodeAccess(node.id)" />
                <span>{{ node.name }} · {{ node.id }}</span>
              </label>
            </div>
            <button class="button primary" type="button" :disabled="selectedUserId == null" @click="saveUserNodes">保存授权</button>
          </div>
        </article>
      </section>
    </DataState>
  </div>
</template>

<style scoped>
.inline-form {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  align-items: end;
  gap: 10px;
  margin-bottom: 16px;
}

.list {
  display: grid;
  gap: 10px;
}

.list-row,
.user-row {
  display: grid;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px;
  background: #fff;
}

.list-row {
  grid-template-columns: 1fr auto auto;
}

.user-row {
  grid-template-columns: 1fr 110px;
}

.user-row.active {
  border-color: var(--green);
  background: var(--green-soft);
}

.user-main {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 0;
  background: transparent;
  text-align: left;
}

.user-main span,
.user-main small {
  display: block;
}

.user-main small {
  color: var(--muted);
}

.auth-box {
  display: grid;
  gap: 10px;
  margin-top: 18px;
  border-top: 1px solid var(--border);
  padding-top: 16px;
}

.node-checks {
  display: grid;
  gap: 8px;
}

.check-row {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--muted-strong);
}

@media (max-width: 760px) {
  .inline-form,
  .list-row,
  .user-row {
    grid-template-columns: 1fr;
  }
}
</style>
