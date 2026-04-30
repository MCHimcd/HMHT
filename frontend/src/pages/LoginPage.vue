<script setup lang="ts">
import { LogIn } from "lucide-vue-next";
import { ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";

import AuthPanel from "../components/AuthPanel.vue";
import { useAuthStore } from "../stores/auth";
import { errorMessage } from "../utils/format";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const username = ref("13800000000");
const password = ref("123456");
const loading = ref(false);
const error = ref("");

async function submit() {
  error.value = "";
  if (!username.value || !password.value) {
    error.value = "请输入手机号和密码";
    return;
  }
  loading.value = true;
  try {
    await auth.login(username.value, password.value);
    await router.push(String(route.query.redirect || "/"));
  } catch (err) {
    error.value = errorMessage(err);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <AuthPanel>
    <form class="auth-form" @submit.prevent="submit">
      <div>
        <p class="eyebrow">登录</p>
        <h1>进入课程演示后台</h1>
        <p class="muted">默认演示用户：13800000000 / 123456</p>
      </div>

      <div v-if="error" class="message error">{{ error }}</div>

      <label class="field">
        <span>手机号</span>
        <input v-model="username" autocomplete="username" placeholder="请输入手机号" />
      </label>
      <label class="field">
        <span>密码</span>
        <input v-model="password" autocomplete="current-password" type="password" placeholder="请输入密码" />
      </label>

      <button class="button primary full" type="submit" :disabled="loading">
        <LogIn :size="18" />
        {{ loading ? "登录中..." : "登录" }}
      </button>

      <div class="auth-links">
        <RouterLink to="/register">注册账号</RouterLink>
        <RouterLink to="/forgot-password">忘记密码</RouterLink>
      </div>
    </form>
  </AuthPanel>
</template>

<style scoped>
.auth-form {
  display: grid;
  gap: 16px;
}

.full {
  width: 100%;
}

.auth-links {
  display: flex;
  justify-content: space-between;
  color: var(--green);
  font-weight: 800;
}
</style>
