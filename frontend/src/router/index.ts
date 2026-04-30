import { createRouter, createWebHistory } from "vue-router";

import { useAuthStore } from "../stores/auth";
import { defaultNodeId } from "../utils/domain";

const authPages = ["/login", "/register", "/forgot-password"];

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", name: "login", component: () => import("../pages/LoginPage.vue") },
    { path: "/register", name: "register", component: () => import("../pages/RegisterPage.vue") },
    { path: "/forgot-password", name: "forgot", component: () => import("../pages/ForgotPasswordPage.vue") },
    { path: "/", redirect: "/home" },
    {
      path: "/home",
      name: "home",
      component: () => import("../pages/DashboardPage.vue"),
      meta: { requiresAuth: true, section: "首页", title: "控制台概览" },
    },
    {
      path: "/gateway",
      name: "gateway",
      component: () => import("../pages/GatewayPage.vue"),
      meta: { requiresAuth: true, section: "设备管理", title: "网关管理" },
    },
    {
      path: "/nodes/:nodeId",
      name: "node-detail",
      component: () => import("../pages/NodeDetailPage.vue"),
      meta: { requiresAuth: true, section: "设备管理", title: "节点详情" },
    },
    {
      path: "/nodes/:nodeId/templates",
      name: "node-templates",
      component: () => import("../pages/EquipmentTemplatePage.vue"),
      meta: { requiresAuth: true, section: "设备管理", title: "设备模板" },
    },
    {
      path: "/nodes/:nodeId/control",
      name: "node-control",
      component: () => import("../pages/DeviceControlPage.vue"),
      meta: { requiresAuth: true, section: "智能控制", title: "设备控制" },
    },
    {
      path: "/nodes/:nodeId/realtime",
      name: "node-realtime",
      component: () => import("../pages/RealtimeDataPage.vue"),
      meta: { requiresAuth: true, section: "数据监控", title: "实时数据" },
    },
    {
      path: "/nodes/:nodeId/history",
      name: "node-history",
      component: () => import("../pages/SensorHistoryPage.vue"),
      meta: { requiresAuth: true, section: "数据监控", title: "历史数据" },
    },
    {
      path: "/nodes/:nodeId/equipment/stats",
      name: "equipment-stats",
      component: () => import("../pages/EquipmentStatsPage.vue"),
      meta: { requiresAuth: true, section: "设备管理", title: "设备统计" },
    },
    {
      path: "/strategies",
      name: "strategies",
      component: () => import("../pages/StrategyPage.vue"),
      meta: { requiresAuth: true, section: "智能控制", title: "自动化策略" },
    },
    {
      path: "/tasks",
      name: "tasks",
      component: () => import("../pages/TasksPage.vue"),
      meta: { requiresAuth: true, section: "任务管理", title: "任务调度" },
    },
    {
      path: "/alerts",
      name: "alerts",
      component: () => import("../pages/AlertsPage.vue"),
      meta: { requiresAuth: true, section: "告警管理", title: "告警中心" },
    },
    {
      path: "/profile",
      name: "profile",
      component: () => import("../pages/ProfilePage.vue"),
      meta: { requiresAuth: true, section: "系统管理", title: "系统设置" },
    },
    {
      path: "/admin",
      name: "admin",
      component: () => import("../pages/AdminPage.vue"),
      meta: { requiresAuth: true, requiresAdmin: true, section: "系统管理", title: "用户管理" },
    },
    {
      path: "/admin/logs",
      name: "logs",
      component: () => import("../pages/LogsPage.vue"),
      meta: { requiresAuth: true, requiresAdmin: true, section: "系统管理", title: "操作日志" },
    },
    { path: "/dashboard", redirect: "/home" },
    { path: "/nodes/:nodeId/sensors", redirect: (to) => `/nodes/${String(to.params.nodeId)}/history` },
    { path: "/nodes/:nodeId/equipment", redirect: (to) => `/nodes/${String(to.params.nodeId)}/control` },
    { path: "/:pathMatch(.*)*", redirect: "/" },
  ],
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (!auth.authReady) {
    await auth.restore();
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }

  if (authPages.includes(to.path) && auth.isAuthenticated) {
    return "/home";
  }

  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return "/home";
  }

  return true;
});
