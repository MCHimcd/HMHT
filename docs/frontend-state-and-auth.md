# Frontend State And Auth

本文定义当前前端的登录态、请求封装、页面状态、错误处理和刷新策略。文件名可随实现调整，但行为应保持一致。

## Auth Model

前端只保存后端返回的业务 token，不自行生成或解析 token。

| State | Source | Purpose |
|-------|--------|---------|
| `token` | `/v1/auth/login` | 写入 `Authorization` 请求头 |
| `tokenExpiresAt` | `Date.now() + expires_in * 1000` | 本地判断是否过期 |
| `profile` | `/v1/auth/profile` | 展示用户信息和菜单权限 |
| `authReady` | 前端初始化流程 | 避免恢复登录态前页面闪烁 |

Recommended local storage keys:

| Key | Content |
|-----|---------|
| `hmht.token` | 登录响应 token |
| `hmht.tokenExpiresAt` | 毫秒时间戳 |
| `hmht.profile` | 最近一次成功获取的用户资料，可选缓存 |

## Restore Flow

1. 应用启动时读取 `hmht.token` 和 `hmht.tokenExpiresAt`。
2. token 不存在或已过期时，清理本地登录态。
3. token 存在且未过期时，调用 `GET /v1/auth/profile`。
4. 资料获取成功后进入已登录状态。
5. 返回 `code=401` 或 token 不可用时，清理登录态并跳转 `/login`。

## Login And Logout

Login:

1. 提交手机号和密码。
2. 调用 `POST /v1/auth/login`。
3. 校验统一响应 `code`。
4. 保存 `token` 与 `tokenExpiresAt`。
5. 拉取 `GET /v1/auth/profile`。
6. 跳转登录前目标页或 `/home`。

Logout:

1. 调用 `POST /v1/auth/logout`。
2. 无论接口成功或失败，都清理 `hmht.token`、`hmht.tokenExpiresAt`、`hmht.profile`。
3. 跳转 `/login`。

后端 token 保存在当前服务实例内，服务重启后登录态可能失效，所以本地缓存不能代表后端仍接受该 token。

## Request Wrapper

所有 API 请求经过统一封装，集中处理基址、认证头、响应解包和业务错误。

```ts
type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

async function request<T>(path: string, options = {}): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: token ? `Bearer ${token}` : "",
      ...options.headers,
    },
  });

  const body = (await response.json()) as ApiResponse<T>;
  if (body.code !== 200) {
    throw new ApiError(body.code, body.message);
  }
  return body.data;
}
```

## Error Handling

| Case | Handling |
|------|----------|
| Network failure | 展示“服务不可用”并提供重试 |
| JSON parse failure | 展示“响应格式异常” |
| `code=400` | 在表单附近展示业务错误 |
| `code=401` | 清理登录态并跳转 `/login` |
| `code=403` | 展示无权限页面或禁用入口 |
| `code=404` | 展示资源不存在状态 |
| `code=409` | 展示冲突原因，例如手机号已注册 |
| Unknown | 展示通用错误消息和重试入口 |

## Page Data State

| State | Meaning |
|-------|---------|
| `idle` | 尚未请求，通常只在首次进入前短暂存在 |
| `loading` | 请求中 |
| `ready` | 已成功获取数据 |
| `empty` | 请求成功但无可展示数据 |
| `error` | 请求失败或业务错误 |

首版不需要全局复杂缓存，但应避免在同一页面内重复请求固定元数据。传感器和设备元数据可缓存到当前页面生命周期。

## Permissions

- 后端是最终权限边界，前端只减少误操作入口。
- 普通用户展示：概览、节点、传感器、设备统计、告警、任务、个人资料。
- 管理员额外展示：用户管理、节点授权、操作日志。
- `profile.role` 为空或未知时按普通用户处理。

## Form Rules

| Form | Local Validation |
|------|------------------|
| 登录 | 手机号和密码非空 |
| 注册 | 手机号、验证码、密码非空 |
| 资料 | 昵称长度合理；性别和出生日期允许为空 |
| 改密 | 旧密码和新密码非空；新密码不应与旧密码相同 |
| 任务 | `hour` 0 到 23，`minute` 0 到 59，重复日期至少一天 |
| 阈值 | 最小值小于最大值；允许空值表示不设置 |

## Time And Format

- 后端健康检查返回 ISO 时间字符串，可直接格式化展示。
- 告警和任务中的数字时间戳按本地时区格式化。
- 课程展示建议统一使用 `YYYY-MM-DD HH:mm`。
- 图表横轴可直接使用接口返回的 `timeLabel`。

## Refresh Strategy

| Page | Strategy |
|------|----------|
| 控制台 | 进入页面时刷新；可提供手动刷新 |
| 节点详情 | 进入页面时刷新节点、元数据、摘要 |
| 历史数据 | 切换指标或范围时刷新 |
| 设备统计 | 切换设备时刷新 |
| 告警列表 | 筛选、分页或处理告警后刷新 |
| 任务管理 | 创建、编辑、删除、启停后刷新 |
| 个人资料 | 更新成功后用返回资料覆盖本地资料 |

首版不需要全局轮询。课程演示需要自动变化时，优先在单页提供手动刷新入口。
