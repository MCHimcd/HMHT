# HMHT 课程版前端状态与认证约定

本文定义首版前端的登录态、请求封装、页面状态和错误处理。实现时可按具体框架调整文件名，但行为应保持一致。

## 登录态模型

前端只维护后端业务 token，不自行生成或解析 token。

| 状态 | 来源 | 用途 |
|------|------|------|
| `token` | `/v1/auth/login` | 写入 `Authorization` 请求头 |
| `tokenExpiresAt` | `Date.now() + expires_in * 1000` | 本地判断是否过期 |
| `profile` | `/v1/auth/profile` | 展示用户信息和菜单权限 |
| `authReady` | 前端初始化过程 | 避免页面在恢复登录态前闪烁 |

建议本地存储键：

| key | 内容 |
|-----|------|
| `hmht.token` | 登录响应 token |
| `hmht.tokenExpiresAt` | 毫秒时间戳 |
| `hmht.profile` | 最近一次成功获取的用户资料，可选缓存 |

## 启动恢复流程

1. 应用启动时读取 `hmht.token` 和 `hmht.tokenExpiresAt`。
2. 如果 token 不存在或已过期，清理本地登录态并进入未登录状态。
3. 如果 token 存在且未过期，调用 `GET /v1/auth/profile`。
4. 如果资料获取成功，进入已登录状态。
5. 如果接口返回 `code=401` 或网络层确认 token 不可用，清理本地登录态并跳转 `/login`。

## 登录流程

1. 用户提交手机号和密码。
2. 调用 `POST /v1/auth/login`。
3. 校验统一响应 `code`。
4. 保存 `token` 与 `tokenExpiresAt`。
5. 拉取 `GET /v1/auth/profile`。
6. 跳转首页或登录前目标页。

## 登出流程

1. 点击登出后调用 `POST /v1/auth/logout`。
2. 无论接口成功或失败，前端都清理 `hmht.token`、`hmht.tokenExpiresAt`、`hmht.profile`。
3. 跳转 `/login`。

这样处理是因为后端 token 存在于当前服务实例内，服务重启后登录态会失效，前端本地缓存不能代表后端仍接受该 token。

## 请求封装

所有 API 请求应经过同一个封装函数，统一处理基址、认证头、响应解包和业务错误。

伪代码：

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

## 错误处理

| 场景 | 处理 |
|------|------|
| 网络失败 | 展示“服务不可用”并提供重试 |
| JSON 解析失败 | 展示“响应格式异常” |
| `code=400` | 表单附近展示业务错误 |
| `code=401` | 清理登录态并跳转 `/login` |
| `code=403` | 展示无权限页面或禁用入口 |
| `code=404` | 展示资源不存在状态 |
| `code=409` | 展示冲突原因，例如手机号已注册 |
| 未知错误 | 展示通用错误消息和重试入口 |

## 页面数据状态

每个页面或数据区维护以下状态：

| 状态 | 说明 |
|------|------|
| `idle` | 尚未请求，通常只在首次进入前短暂存在 |
| `loading` | 请求中 |
| `ready` | 已成功获取数据 |
| `empty` | 请求成功但无可展示数据 |
| `error` | 请求失败或业务错误 |

首版不要求全局复杂缓存，但应避免同一页面重复请求同一份固定元数据。建议把传感器元数据和设备元数据缓存在当前页面生命周期内。

## 权限与菜单

- 后端是最终权限边界，前端只负责减少误操作入口。
- 普通用户展示：概览、节点、传感器、设备统计、告警、任务、个人资料。
- 管理员额外展示：管理入口、操作日志入口。
- 如果 `profile.role` 为空或未知，按普通用户处理。

## 表单状态

| 表单 | 本地校验 |
|------|----------|
| 登录 | 手机号和密码非空 |
| 注册 | 手机号、验证码、密码非空 |
| 资料 | 昵称长度合理；性别和出生日期允许为空 |
| 改密 | 旧密码和新密码非空；新密码不应与旧密码相同 |
| 任务 | `hour` 为 0 到 23，`minute` 为 0 到 59，重复日期至少选择一天 |
| 阈值 | 最小值小于最大值；允许传空值表示不设置 |

## 时间与格式

- 后端健康检查返回 ISO 时间字符串，可直接格式化展示。
- 告警和任务中的时间戳为数字时，前端按本地时区格式化。
- 课程展示建议统一使用 `YYYY-MM-DD HH:mm`，图表横轴可直接使用接口 `timeLabel`。

## 数据刷新

| 页面 | 刷新策略 |
|------|----------|
| 首页 | 进入页面时刷新；可提供手动刷新按钮 |
| 节点详情 | 进入页面时刷新元数据 |
| 传感器曲线 | 切换指标或范围时刷新 |
| 设备统计 | 切换设备时刷新 |
| 告警列表 | 筛选、分页或处理告警后刷新 |
| 任务管理 | 创建、编辑、删除、启停后刷新 |
| 个人资料 | 更新成功后使用返回资料覆盖本地资料 |

首版不需要轮询。如果课程演示需要自动变化，可只对首页或曲线页增加手动刷新入口。
