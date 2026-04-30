# HMHT 课程版前端环境与运行约定

本文定义首版前端的推荐技术选型、本地运行、环境变量和后端联调方式。当前仓库仍是 Spring Boot 后端仓库，前端实现时建议放入仓库根目录下的 `frontend/`。

## 推荐选型

| 项 | 建议 |
|----|------|
| 构建工具 | Vite |
| 框架 | Vue 3 + TypeScript |
| 包管理 | npm |
| 图表 | ECharts |
| 路由 | Vue Router |
| 状态 | Pinia |
| HTTP | 原生 `fetch` 封装即可 |
| 样式 | CSS Modules、普通 CSS 或轻量组件库，保持简单 |

选择 Vite + Vue 3 的原因是启动快、工程轻、对表单和后台页面更直观，适合当前课程版项目快速联调和演示。Pinia 与 Vue Router 都是官方主流组合，适合把登录态、页面路由和业务页面结构保持清晰。

## 目录建议

```text
frontend/
├── index.html
├── package.json
├── vite.config.ts
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── api/
│   │   ├── client.ts
│   │   ├── auth.ts
│   │   ├── nodes.ts
│   │   ├── sensors.ts
│   │   ├── alerts.ts
│   │   └── tasks.ts
│   ├── stores/
│   │   └── auth.ts
│   ├── router/
│   │   └── index.ts
│   ├── pages/
│   ├── components/
│   └── styles/
└── .env.example
```

## 本地端口

| 服务 | 默认端口 | 地址 |
|------|----------|------|
| 后端 | `8080` | `http://localhost:8080/api` |
| 前端 | `5173` | `http://localhost:5173` |
| MySQL | `3306` | 由后端配置连接 |

## 环境变量

建议在 `frontend/.env.example` 中提供：

```dotenv
VITE_API_BASE_URL=/api
```

本地开发可复制为 `frontend/.env.local` 后按需修改。

| 变量 | 说明 | 示例 |
|------|------|------|
| `VITE_API_BASE_URL` | 后端 API 基址，必须包含 `/api`；本地默认走 Vite 代理 | `/api` |

## 后端启动

在仓库根目录启动后端：

```bash
./gradlew bootRun
```

后端健康检查：

```bash
curl http://localhost:8080/api/v1/health
```

预期返回统一响应，`data.edition` 为 `course`。

## 前端启动

前端创建完成后，在 `frontend/` 目录运行：

```bash
npm install
npm run dev
```

浏览器访问：

```text
http://localhost:5173
```

当前前端已按本文档落地为 `frontend/` 下的 Vite + Vue 3 + TypeScript 工程，包含 Pinia 登录态、Vue Router 路由、ECharts 图表、Vitest 测试和全量课程演示页面。

## 开发代理

前端工程已配置 Vite 开发代理，默认 `VITE_API_BASE_URL=/api` 会转发到本地后端 `http://127.0.0.1:8080`。

如果希望开发环境统一请求 `/api`，可在 `vite.config.ts` 配置代理：

```ts
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
```

使用代理时，`VITE_API_BASE_URL` 可设置为：

```dotenv
VITE_API_BASE_URL=/api
```

## 构建

前端创建完成后建议提供以下脚本：

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "preview": "vite preview",
    "lint": "eslint ."
  }
}
```

本地构建：

```bash
npm run build
```

预览构建产物：

```bash
npm run preview
```

## 联调检查清单

| 检查项 | 命令或页面 |
|--------|------------|
| 后端可用 | `curl http://localhost:8080/api/v1/health` |
| 登录可用 | 使用 `13800000000` / `123456` 登录 |
| 管理员可用 | 使用 `13800000001` / `admin123` 登录 |
| 节点可见 | 首页展示 `SIM_Node_01` |
| 曲线可见 | 传感器曲线页有 `points` 数据 |
| 告警可处理 | 告警列表处理后刷新 |
| 任务可维护 | 任务新增、启停、删除流程可完成 |
| 前端构建 | `cd frontend && npm run build` |
| 前端测试 | `cd frontend && npm run test` |

## 部署说明

当前 `docs/deployment.md` 只覆盖 Spring Boot 后端部署。前端部署有两种简单方式：

| 方式 | 说明 |
|------|------|
| 独立静态站点 | 将 `frontend/dist/` 部署到 Nginx、对象存储或任意静态站点服务 |
| 后端同机 Nginx | Nginx 托管前端静态文件，并反向代理 `/api` 到 `localhost:8080` |

课程演示阶段优先使用本地运行；正式部署前再补充前端部署脚本和 Nginx 示例。
