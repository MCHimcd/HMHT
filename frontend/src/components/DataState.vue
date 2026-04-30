<script setup lang="ts">
defineProps<{
  status: "idle" | "loading" | "ready" | "empty" | "error";
  message?: string;
}>();

const emit = defineEmits<{
  retry: [];
}>();
</script>

<template>
  <div v-if="status === 'loading'" class="state-card">
    <div class="pulse" />
    <p>正在加载课程数据...</p>
  </div>
  <div v-else-if="status === 'error'" class="state-card error">
    <p>{{ message || "加载失败" }}</p>
    <button class="button" type="button" @click="emit('retry')">重试</button>
  </div>
  <div v-else-if="status === 'empty'" class="empty-state">
    <p>{{ message || "暂无可展示数据" }}</p>
  </div>
  <slot v-else />
</template>

<style scoped>
.state-card {
  display: grid;
  place-items: center;
  min-height: 180px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  color: var(--muted);
}

.state-card.error {
  color: var(--red);
}

.pulse {
  width: 34px;
  height: 34px;
  border: 4px solid var(--green-soft);
  border-top-color: var(--green);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
