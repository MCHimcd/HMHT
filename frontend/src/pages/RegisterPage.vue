<script setup lang="ts">
import { UserPlus } from "lucide-vue-next";
import { ref } from "vue";
import { RouterLink, useRouter } from "vue-router";

import * as authApi from "../api/auth";
import AuthPanel from "../components/AuthPanel.vue";
import { errorMessage } from "../utils/format";

const router = useRouter();
const phone = ref("13800000002");
const verificationCode = ref("");
const password = ref("123456");
const loading = ref(false);
const sending = ref(false);
const error = ref("");
const success = ref("");
const testCode = ref("");

async function sendCode() {
  error.value = "";
  success.value = "";
  if (!phone.value) {
    error.value = "请输入手机号";
    return;
  }
  sending.value = true;
  try {
    const result = await authApi.sendVerificationCode(phone.value);
    testCode.value = result.test_code;
    verificationCode.value = result.test_code;
    success.value = `测试验证码：${result.test_code}，${result.expires_in} 秒内有效`;
  } catch (err) {
    error.value = errorMessage(err);
  } finally {
    sending.value = false;
  }
}

async function submit() {
  error.value = "";
  success.value = "";
  if (!phone.value || !verificationCode.value || !password.value) {
    error.value = "手机号、验证码和密码均不能为空";
    return;
  }
  loading.value = true;
  try {
    await authApi.register(phone.value, verificationCode.value, password.value);
    success.value = "注册成功，正在返回登录页";
    setTimeout(() => void router.push("/login"), 650);
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
        <p class="eyebrow">注册</p>
        <h1>创建课程演示账号</h1>
        <p class="muted">课程版会直接返回测试验证码，便于课堂联调。</p>
      </div>

      <div v-if="error" class="message error">{{ error }}</div>
      <div v-if="success" class="message success">{{ success }}</div>

      <label class="field">
        <span>手机号</span>
        <input v-model="phone" placeholder="请输入手机号" />
      </label>
      <div class="code-row">
        <label class="field">
          <span>验证码</span>
          <input v-model="verificationCode" placeholder="请输入验证码" />
        </label>
        <button class="button" type="button" :disabled="sending" @click="sendCode">
          {{ sending ? "发送中..." : "获取验证码" }}
        </button>
      </div>
      <label class="field">
        <span>密码</span>
        <input v-model="password" type="password" placeholder="至少 6 位" />
      </label>

      <button class="button primary full" type="submit" :disabled="loading">
        <UserPlus :size="18" />
        {{ loading ? "注册中..." : "注册" }}
      </button>

      <p v-if="testCode" class="muted">当前自动填入验证码 {{ testCode }}</p>
      <RouterLink class="back-link" to="/login">返回登录</RouterLink>
    </form>
  </AuthPanel>
</template>

<style scoped>
.auth-form {
  display: grid;
  gap: 16px;
}

.code-row {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: 10px;
}

.full {
  width: 100%;
}

.back-link {
  color: var(--green);
  font-weight: 800;
}

@media (max-width: 520px) {
  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
