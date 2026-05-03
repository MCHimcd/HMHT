# Frontend Environment And Runbook

本文记录当前 `frontend/` 工程的技术选型、本地运行、环境变量、代理和构建验证方式。

## Stack

| Area | Choice |
|------|--------|
| Build tool | Vite |
| Framework | Vue 3 + TypeScript |
| Package manager | npm |
| Charts | ECharts |
| Router | Vue Router |
| State | Pinia |
| Icons | lucide-vue-next |
| Test | Vitest + Vue Test Utils + jsdom |

## Directory

```text
frontend/
├── index.html
├── package.json
├── vite.config.ts
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── api/
│   ├── stores/
│   ├── router/
│   ├── pages/
│   ├── components/
│   ├── styles/
│   └── test/
└── .env.example
```

## Local Ports

| Service | Default | URL |
|---------|---------|-----|
| Backend | `8080` | `http://localhost:8080/api` |
| Frontend | `5173` | `http://localhost:5173` |
| MySQL | `3306` | configured by backend datasource |

## Environment Variables

`frontend/.env.example`:

```dotenv
VITE_API_BASE_URL=/api
```

| Variable | Purpose | Local Value |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | 后端 API 基址，必须包含 `/api` | `/api` |

Local development normally keeps `VITE_API_BASE_URL=/api` and relies on the Vite proxy.

## Backend

Start backend from repository root:

```bash
./gradlew bootRun
```

Health check:

```bash
curl http://localhost:8080/api/v1/health
```

Expected response has `code=200` and `data.edition="course"`.

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

## Dev Proxy

`vite.config.ts` proxies `/api` to `http://127.0.0.1:8080`, so browser requests can use same-origin `/api` paths during development.

Reference shape:

```ts
server: {
  port: 5173,
  proxy: {
    "/api": {
      target: "http://127.0.0.1:8080",
      changeOrigin: true,
    },
  },
}
```

## Scripts

Current scripts from `frontend/package.json`:

| Script | Command | Purpose |
|--------|---------|---------|
| `dev` | `vite` | 本地开发服务器 |
| `build` | `vue-tsc -b && vite build` | 类型检查并生成生产构建 |
| `preview` | `vite preview` | 本地预览 `dist/` |
| `test` | `vitest run` | 运行前端测试 |
| `lint` | `vue-tsc -b --noEmit` | 类型检查 |

## Integration Checklist

| Check | Command / Page |
|-------|----------------|
| Backend health | `curl http://localhost:8080/api/v1/health` |
| User login | `13800000000` / `123456` |
| Admin login | `13800000001` / `admin123` |
| Node visible | 控制台或节点详情展示 `SIM_Node_01` |
| Sensor chart | 历史数据页有 `points` |
| Equipment stats | 设备统计页有趋势数据 |
| Alert handling | 告警处理后列表刷新，未处理计数与铃铛角标同步 |
| Task CRUD | 任务新增、编辑、启停、删除可完成 |
| Frontend build | `cd frontend && npm run build` |
| Frontend test | `cd frontend && npm run test` |

## Deployment

`docs/deployment.md` covers backend deployment through GitHub Actions, SSH, and systemd. Frontend deployment has two simple options:

| Mode | Notes |
|------|-------|
| Static site | Deploy `frontend/dist/` to Nginx, object storage, or any static hosting service |
| Same server Nginx | Nginx serves static files and reverse-proxies `/api` to `localhost:8080` |

For classroom demos, local frontend + local backend is the preferred path.

Realtime views and the console top bar poll every 2 seconds. Sensor charts use a rolling simulated history: older points stay stable, and each refresh appends only the newest point. The simulated air-temperature stream periodically produces a value below `5C`, which generates a low-temperature warning after a short backend cooldown.
