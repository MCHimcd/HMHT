<script setup lang="ts">
import { KeyRound } from "lucide-vue-next";
import { ref } from "vue";
import { RouterLink, useRouter } from "vue-router";

import * as authApi from "../api/auth";
import AuthPanel from "../components/AuthPanel.vue";
import { errorMessage } from "../utils/format";

const router = useRouter();
const phone = ref("13800000000");
const verificationCode = ref("");
const newPassword = ref("");
const error = ref("");
const success = ref("");
const sending = ref(false);
const loading = ref(false);

async function sendCode() {
  error.value = "";
  success.value = "";
  sending.value = true;
  try {
    const result = await authApi.sendVerificationCode(phone.value);
    verificationCode.value = result.test_code;
    success.value = `测试验证码：${result.test_code}`;
  } catch (err) {
    error.value = errorMessage(err);
  } finally {
    sending.value = false;
  }
}

async function submit() {
  error.value = "";
  success.value = "";
  if (!phone.value || !verificationCode.value || !newPassword.value) {
    error.value = "手机号、验证码和新密码均不能为空";
    return;
  }
  loading.value = true;
  try {
    await authApi.forgotPassword(phone.value, verificationCode.value, newPassword.value);
    success.value = "密码已重置，正在返回登录页";
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
        <p class="eyebrow">找回密码</p>
        <h1>通过测试验证码重置密码</h1>
        <p class="muted">适合课程演示完整认证闭环。</p>
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
        <span>新密码</span>
        <input v-model="newPassword" type="password" placeholder="请输入新密码" />
      </label>
      <button class="button primary full" type="submit" :disabled="loading">
        <KeyRound :size="18" />
        {{ loading ? "提交中..." : "重置密码" }}
      </button>
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
