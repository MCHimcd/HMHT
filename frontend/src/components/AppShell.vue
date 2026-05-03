<script setup lang="ts">
import {
  Activity,
  Bell,
  CalendarClock,
  ChevronDown,
  History,
  Home,
  LogOut,
  Menu,
  Network,
  PanelLeft,
  Router,
  Settings,
  SlidersHorizontal,
  Sprout,
  Thermometer,
  UserRound,
  UsersRound,
  Waves,
  Workflow,
} from "lucide-vue-next";
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";

import { listAlerts } from "../api/alerts";
import { getSensorHistory } from "../api/sensors";
import { useAuthStore } from "../stores/auth";
import { formatMetricValue, latestMetricValue } from "../utils/console";
import { defaultNodeId } from "../utils/domain";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const mobileOpen = ref(false);
const collapsed = ref(false);
const userMenuOpen = ref(false);
const unhandledCount = ref(0);
const quickMetrics = ref([
  { label: "空气温度", value: "-", icon: Thermometer, color: "#1884e8" },
  { label: "空气湿度", value: "-", icon: Activity, color: "#00a870" },
  { label: "土壤湿度", value: "-", icon: Waves, color: "#0aa55c" },
]);
let summaryTimer: number | undefined;

const currentNodeId = computed(() => String(route.params.nodeId || defaultNodeId));
const sectionLabel = computed(() => String(route.meta.section || "控制台"));
const titleLabel = computed(() => String(route.meta.title || "页面"));

const navGroups = computed(() => [
  {
    title: "",
    items: [{ routeName: "home", label: "首页", icon: Home }],
  },
  {
    title: "设备管理",
    items: [
      { routeName: "node-detail", label: "节点管理", icon: Network, needsNode: true },
      { routeName: "gateway", label: "网关管理", icon: Router },
      { routeName: "node-templates", label: "设备模板", icon: SlidersHorizontal, needsNode: true },
    ],
  },
  {
    title: "数据监控",
    items: [
      { routeName: "node-realtime", label: "实时数据", icon: Activity, needsNode: true },
      { routeName: "node-history", label: "历史数据", icon: History, needsNode: true },
    ],
  },
  {
    title: "智能控制",
    items: [
      { routeName: "node-control", label: "设备控制", icon: SlidersHorizontal, needsNode: true },
      { routeName: "strategies", label: "自动化策略", icon: Workflow },
    ],
  },
  {
    title: "任务管理",
    items: [{ routeName: "tasks", label: "任务调度", icon: CalendarClock }],
  },
  {
    title: "告警管理",
    items: [{ routeName: "alerts", label: "告警中心", icon: Bell }],
  },
  {
    title: "系统管理",
    items: [
      ...(auth.isAdmin ? [{ routeName: "admin", label: "用户管理", icon: UsersRound }] : []),
      ...(auth.isAdmin ? [{ routeName: "logs", label: "操作日志", icon: UserRound }] : []),
      { routeName: "profile", label: "系统设置", icon: Settings },
    ],
  },
]);

function navTarget(item: { routeName: string; needsNode?: boolean }) {
  return item.needsNode ? { name: item.routeName, params: { nodeId: currentNodeId.value } } : { name: item.routeName };
}

function isActive(item: { routeName: string }) {
  return route.name === item.routeName;
}

async function loadConsoleSummary() {
  try {
    const [temperature, humidity, soilHumidity, alertResult] = await Promise.all([
      getSensorHistory(currentNodeId.value, "temperature", "24h"),
      getSensorHistory(currentNodeId.value, "humidity", "24h"),
      getSensorHistory(currentNodeId.value, "soilHumidity", "24h"),
      listAlerts({ nodeId: currentNodeId.value, handled: false, page: 1, size: 5 }),
    ]);
    quickMetrics.value = [
      {
        label: "空气温度",
        value: `${formatMetricValue(latestMetricValue({ temperature }, "temperature"), "temperature")}°C`,
        icon: Thermometer,
        color: "#1884e8",
      },
      {
        label: "空气湿度",
        value: `${formatMetricValue(latestMetricValue({ humidity }, "humidity"), "humidity")}%`,
        icon: Activity,
        color: "#00a870",
      },
      {
        label: "土壤湿度",
        value: `${formatMetricValue(latestMetricValue({ soilHumidity }, "soilHumidity"), "soilHumidity")}%`,
        icon: Waves,
        color: "#0aa55c",
      },
    ];
    unhandledCount.value = alertResult.unhandled_count;
  } catch {
    unhandledCount.value = 0;
  }
}

watch(currentNodeId, () => void loadConsoleSummary(), { immediate: true });
watch(
  () => route.fullPath,
  () => {
    mobileOpen.value = false;
    userMenuOpen.value = false;
  },
);

async function handleLogout() {
  userMenuOpen.value = false;
  await auth.logout();
  await router.push("/login");
}

onMounted(() => {
  summaryTimer = window.setInterval(() => void loadConsoleSummary(), 2000);
});

onBeforeUnmount(() => {
  if (summaryTimer) window.clearInterval(summaryTimer);
});
</script>

<template>
  <div class="console-shell" :class="{ collapsed }">
    <aside class="console-sidebar" :class="{ open: mobileOpen, collapsed }">
      <RouterLink class="console-brand" :to="{ name: 'home' }" @click="mobileOpen = false">
        <div class="console-brand-mark"><Sprout :size="30" stroke-width="2.4" /></div>
        <div class="console-brand-copy">
          <strong>HMHT</strong>
          <span>智慧农业课程版</span>
        </div>
      </RouterLink>

      <nav class="console-nav" aria-label="主导航">
        <section v-for="group in navGroups" :key="group.title || 'home'" class="console-nav-group">
          <p v-if="group.title" class="console-nav-title">{{ group.title }}</p>
          <RouterLink
            v-for="item in group.items"
            :key="`${group.title}-${item.label}`"
            class="console-nav-link"
            :class="{ active: isActive(item) }"
            :to="navTarget(item)"
            @click="mobileOpen = false"
          >
            <component :is="item.icon" :size="18" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </section>
      </nav>

      <button class="collapse-button" type="button" @click="collapsed = !collapsed">
        <PanelLeft :size="17" />
        <span>{{ collapsed ? "展开菜单" : "收起菜单" }}</span>
      </button>
    </aside>

    <div v-if="mobileOpen" class="scrim" @click="mobileOpen = false" />

    <main class="console-workspace">
      <header class="console-topbar">
        <div class="console-breadcrumb">
          <button class="top-icon menu-button" type="button" aria-label="打开导航" @click="mobileOpen = true">
            <Menu :size="19" />
          </button>
          <span>{{ sectionLabel }}</span>
          <em>/</em>
          <strong>{{ titleLabel }}</strong>
        </div>

        <div class="console-statusbar" aria-label="控制台状态">
          <span v-for="item in quickMetrics" :key="item.label">
            <component :is="item.icon" :size="18" :color="item.color" />{{ item.label }}
            <strong>{{ item.value }}</strong>
          </span>
          <RouterLink class="console-status-link notification" :to="{ name: 'alerts' }" title="查看告警中心" aria-label="查看告警中心">
            <Bell :size="18" />
            <i v-if="unhandledCount > 0">{{ unhandledCount }}</i>
          </RouterLink>
          <div class="console-user-menu">
            <button class="console-user" type="button" :aria-expanded="userMenuOpen" @click="userMenuOpen = !userMenuOpen">
              <UserRound :size="22" />{{ auth.profile?.role === "admin" ? "管理员" : "普通用户" }}<small>{{ auth.displayName }}</small>
              <ChevronDown class="user-chevron" :class="{ open: userMenuOpen }" :size="14" />
            </button>
            <div v-if="userMenuOpen" class="user-popover">
              <RouterLink :to="{ name: 'profile' }">系统设置</RouterLink>
              <button type="button" @click="handleLogout">退出登录</button>
            </div>
          </div>
          <button class="top-icon" type="button" title="退出登录" aria-label="退出登录" @click="handleLogout">
            <LogOut :size="17" />
          </button>
        </div>
      </header>

      <section class="console-content">
        <slot />
      </section>

      <footer class="console-footer">
        <span>HMHT 智慧农业课程版 v2.1.0</span>
        <span>© 2026 HMHT 团队</span>
        <span>帮助文档</span>
        <span>技术支持</span>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.console-shell {
  display: grid;
  grid-template-columns: 214px minmax(0, 1fr);
  min-height: 100vh;
  background: #f6f8f9;
  transition: grid-template-columns 0.18s ease;
}

.console-shell.collapsed {
  grid-template-columns: 82px minmax(0, 1fr);
}

.console-sidebar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: grid;
  grid-template-rows: 64px 1fr 54px;
  height: 100vh;
  overflow: hidden;
  border-right: 1px solid #e5ebef;
  background: #fff;
  box-shadow: 4px 0 18px rgba(15, 35, 25, 0.04);
  transition: transform 0.18s ease;
}

.console-brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 20px;
  background: linear-gradient(135deg, #0a9a57 0%, #078143 100%);
  color: #fff;
}

.console-brand-mark {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
}

.console-brand-copy strong,
.console-brand-copy span {
  display: block;
}

.console-brand-copy strong {
  font-size: 21px;
  line-height: 1;
}

.console-brand-copy span {
  margin-top: 5px;
  font-size: 12px;
  font-weight: 800;
}

.console-nav {
  overflow: auto;
  padding: 14px 10px;
}

.console-nav-group {
  display: grid;
  gap: 5px;
  margin-bottom: 18px;
}

.console-nav-title {
  margin: 0 0 5px 10px;
  color: #7b8792;
  font-size: 12px;
  font-weight: 750;
}

.console-nav-link {
  display: flex;
  align-items: center;
  gap: 11px;
  min-height: 42px;
  border-radius: 4px;
  padding: 0 13px;
  color: #31404c;
  font-size: 14px;
  font-weight: 750;
}

.console-nav-link svg {
  color: #263845;
}

.console-nav-link:hover,
.console-nav-link.active {
  background: #eaf7ef;
  color: #048548;
}

.console-nav-link:hover svg,
.console-nav-link.active svg {
  color: #048548;
}

.collapse-button {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 0;
  border-top: 1px solid #e5ebef;
  background: #fff;
  padding: 0 20px;
  color: #5f6f7b;
  font-weight: 750;
}

.console-sidebar.collapsed .console-brand {
  justify-content: center;
  padding: 0;
}

.console-sidebar.collapsed .console-brand-copy,
.console-sidebar.collapsed .console-nav-title,
.console-sidebar.collapsed .console-nav-link span,
.console-sidebar.collapsed .collapse-button span {
  display: none;
}

.console-sidebar.collapsed .console-nav-link,
.console-sidebar.collapsed .collapse-button {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
}

.console-workspace {
  display: grid;
  grid-template-rows: 64px 1fr 46px;
  min-width: 0;
  min-height: 100vh;
}

.console-topbar {
  position: sticky;
  top: 0;
  z-index: 12;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border-bottom: 1px solid #e5ebef;
  background: rgba(255, 255, 255, 0.96);
  padding: 0 22px;
  backdrop-filter: blur(12px);
}

.console-breadcrumb,
.console-statusbar,
.console-statusbar span,
.console-user {
  display: flex;
  align-items: center;
}

.console-breadcrumb {
  gap: 14px;
  color: #7c8791;
  font-size: 14px;
  white-space: nowrap;
}

.console-breadcrumb strong {
  color: #283540;
}

.console-breadcrumb em {
  color: #c1c9cf;
  font-style: normal;
}

.console-statusbar {
  gap: 18px;
  color: #26333e;
  font-size: 13px;
}

.console-statusbar > span,
.console-status-link {
  gap: 7px;
  min-height: 30px;
  border-left: 1px solid #e4eaee;
  padding-left: 18px;
}

.console-statusbar > span:first-child {
  border-left: 0;
  padding-left: 0;
}

.console-status-link {
  position: relative;
  display: inline-flex;
  align-items: center;
  color: #26333e;
}

.notification i {
  position: absolute;
  top: -8px;
  right: -10px;
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #ff4d4f;
  color: #fff;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
}

.console-user {
  gap: 7px;
  border: 0;
  border-left: 1px solid #e4eaee;
  background: transparent;
  color: #26333e;
  font-weight: 850;
  min-height: 30px;
  padding-left: 18px;
}

.console-user small {
  color: #6a757e;
  font-size: 12px;
  font-weight: 700;
}

.console-user-menu {
  position: relative;
}

.user-chevron {
  transition: transform 0.16s ease;
}

.user-chevron.open {
  transform: rotate(180deg);
}

.user-popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 30;
  display: grid;
  min-width: 132px;
  border: 1px solid #d9e7de;
  border-radius: 8px;
  padding: 6px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(15, 35, 25, 0.14);
}

.user-popover a,
.user-popover button {
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #26333e;
  padding: 9px 10px;
  text-align: left;
  font-weight: 750;
}

.user-popover a:hover,
.user-popover button:hover,
.console-user:hover {
  background: #edf3f0;
}

.top-icon {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #26333e;
}

.top-icon:hover,
.console-status-link:hover {
  background: #edf3f0;
}

.menu-button {
  display: none;
}

.console-content {
  min-width: 0;
  padding: 34px 30px 24px;
}

.console-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 22px;
  border-top: 1px solid #e5ebef;
  background: #fff;
  color: #7c8791;
  font-size: 13px;
}

.scrim {
  position: fixed;
  inset: 0;
  z-index: 18;
  background: rgba(8, 28, 18, 0.28);
}

@media (max-width: 1120px) {
  .console-shell,
  .console-shell.collapsed {
    grid-template-columns: 1fr;
  }

  .console-sidebar {
    position: fixed;
    left: 0;
    width: 214px;
    transform: translateX(-100%);
  }

  .console-sidebar.open {
    transform: translateX(0);
  }

  .console-sidebar.collapsed .console-brand-copy,
  .console-sidebar.collapsed .console-nav-title,
  .console-sidebar.collapsed .console-nav-link span,
  .console-sidebar.collapsed .collapse-button span {
    display: block;
  }

  .menu-button {
    display: inline-grid;
  }

  .console-statusbar {
    display: none;
  }
}

@media (max-width: 720px) {
  .console-content {
    padding: 18px 14px;
  }

  .console-footer {
    display: none;
  }

  .console-workspace {
    grid-template-rows: 58px 1fr;
  }
}
</style>
