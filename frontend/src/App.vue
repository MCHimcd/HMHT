<script setup lang="ts">
import { computed, onMounted } from "vue";
import { RouterView, useRoute } from "vue-router";

import AppShell from "./components/AppShell.vue";
import { useAuthStore } from "./stores/auth";

const route = useRoute();
const auth = useAuthStore();

const isAuthRoute = computed(() => ["/login", "/register", "/forgot-password"].includes(route.path));

onMounted(() => {
  if (!auth.authReady) {
    void auth.restore();
  }
});
</script>

<template>
  <RouterView v-if="isAuthRoute" />
  <AppShell v-else>
    <RouterView />
  </AppShell>
</template>
