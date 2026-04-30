<script setup lang="ts">
import { LogOut, Save } from "lucide-vue-next";
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";

import * as authApi from "../api/auth";
import { useAuthStore } from "../stores/auth";
import { errorMessage } from "../utils/format";

const auth = useAuthStore();
const router = useRouter();
const message = ref("");
const passwordMessage = ref("");
const loading = ref(false);

const profileForm = reactive({
  userName: "",
  gender: "",
  birthDate: "",
  avatarSrc: "",
});

const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
});

function syncProfile() {
  profileForm.userName = auth.profile?.userName || "";
  profileForm.gender = auth.profile?.gender || "";
  profileForm.birthDate = auth.profile?.birthDate || "";
  profileForm.avatarSrc = auth.profile?.avatarSrc || "";
}

async function saveProfile() {
  message.value = "";
  if (!profileForm.userName.trim()) {
    message.value = "昵称不能为空";
    return;
  }
  loading.value = true;
  try {
    await auth.updateProfile({ ...profileForm });
    message.value = "资料已保存";
  } catch (err) {
    message.value = errorMessage(err);
  } finally {
    loading.value = false;
  }
}

async function changePassword() {
  passwordMessage.value = "";
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    passwordMessage.value = "旧密码和新密码不能为空";
    return;
  }
  if (passwordForm.oldPassword === passwordForm.newPassword) {
    passwordMessage.value = "新密码不能与旧密码相同";
    return;
  }
  try {
    await authApi.changePassword(passwordForm.oldPassword, passwordForm.newPassword);
    passwordForm.oldPassword = "";
    passwordForm.newPassword = "";
    passwordMessage.value = "密码已修改";
  } catch (err) {
    passwordMessage.value = errorMessage(err);
  }
}

async function logout() {
  await auth.logout();
  await router.push("/login");
}

onMounted(syncProfile);
</script>

<template>
  <div class="page-stack">
    <header class="page-head">
      <div>
        <p class="eyebrow">Profile</p>
        <h1>个人资料</h1>
        <p class="muted">读取和更新登录用户资料，支持修改密码与退出登录。</p>
      </div>
      <button class="button" type="button" @click="logout"><LogOut :size="17" />退出登录</button>
    </header>

    <section class="grid two">
      <form class="card pad form" @submit.prevent="saveProfile">
        <div>
          <h2>基本资料</h2>
          <p class="muted">{{ auth.profile?.phone }} · {{ auth.profile?.role || "user" }}</p>
        </div>
        <div v-if="message" class="message" :class="message.includes('已') ? 'success' : 'error'">{{ message }}</div>
        <label class="field">
          <span>昵称</span>
          <input v-model="profileForm.userName" maxlength="30" />
        </label>
        <label class="field">
          <span>性别</span>
          <select v-model="profileForm.gender">
            <option value="">不设置</option>
            <option value="male">男</option>
            <option value="female">女</option>
          </select>
        </label>
        <label class="field">
          <span>出生日期</span>
          <input v-model="profileForm.birthDate" type="date" />
        </label>
        <label class="field">
          <span>头像地址</span>
          <input v-model="profileForm.avatarSrc" placeholder="课程版不做上传，可填写 URL" />
        </label>
        <button class="button primary" type="submit" :disabled="loading"><Save :size="17" />保存资料</button>
      </form>

      <form class="card pad form" @submit.prevent="changePassword">
        <div>
          <h2>修改密码</h2>
          <p class="muted">成功后当前 token 仍可用；如需重新登录可手动退出。</p>
        </div>
        <div v-if="passwordMessage" class="message" :class="passwordMessage.includes('已') ? 'success' : 'error'">{{ passwordMessage }}</div>
        <label class="field">
          <span>旧密码</span>
          <input v-model="passwordForm.oldPassword" type="password" autocomplete="current-password" />
        </label>
        <label class="field">
          <span>新密码</span>
          <input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" />
        </label>
        <button class="button primary" type="submit">修改密码</button>
      </form>
    </section>
  </div>
</template>

<style scoped>
.form {
  display: grid;
  gap: 14px;
  align-content: start;
}
</style>
