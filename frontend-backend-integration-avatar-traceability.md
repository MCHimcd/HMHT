# Wangnong 前后端接口对接说明（扫码 + 头像）

更新时间：2026-04-07  
适用前端版本：本次 Wangnong 前端改造（TraceabilityScanPage + AvatarCropPage + ProfileView）

## 1. 结论总览

本次前端改造后，后端对接要求如下：

1. 继续沿用现有溯源接口 `POST /api/v1/traceability/scan`（无需改路径）。
2. 必须新增头像上传接口 `POST /api/v1/auth/avatar/upload`（multipart/form-data）。
3. 用户资料返回体 `UserProfile` 的 `avatarSrc` 字段必须可直接用于前端 `Image()` 展示。
4. 建议统一错误码和响应体结构为 `BaseResponse<T> { code, message, data }`，与现有前端 HttpService 对齐。

---

## 2. 前端已实现行为（后端需知）

### 2.1 溯源码扫码页面

前端已支持两条识别路径：

1. 页面内实时摄像头扫码（不跳转独立扫码页，流程在当前页面闭环）
2. 相册选图后本地识别二维码

两条路径都会在前端得到二维码文本（如 `TRACE_2026_001`），然后统一调用：

- `POST /api/v1/traceability/scan`
- Body: `{ "code": "TRACE_2026_001" }`

说明：前端会做 `trim + upperCase` 归一化。

界面行为说明：

1. 页面顶部保留返回箭头、标题、`从相册选择` 按钮。
2. 页面主体仅显示实时摄像头画面 + 扫码框 + 引导提示，不再显示手动输入与上传按钮。
3. 二维码识别成功后由前端自动调用溯源接口。

### 2.2 头像修改页面

前端已实现：

1. 个人页头像菜单二选一：拍照 / 从相册选择
2. 进入独立裁剪页，支持圆形裁剪框拖动 + 缩放
3. 前端生成圆形头像 PNG（透明背景）
4. 上传裁剪结果到后端头像接口
5. 上传成功后，以后端返回的 `avatarSrc` 更新全局状态

如果后端未实现接口，前端会本地替换头像并提示“后端上传失败，已本地替换头像”。

拍照失败诊断策略：

1. 用户取消拍照：不提示失败。
2. 相机权限未授予：提示用户授权后重试。
3. 设备/模拟器不支持拍照：提示“当前环境可能不支持拍照（常见于模拟器），请在真机测试或改用从相册选择”。
4. 其他未知异常：统一提示“拍照失败，请重试”。

---

## 3. 后端必须新增接口

## 3.1 上传头像

- Method: `POST`
- Path: `/api/v1/auth/avatar/upload`
- Auth: `Authorization: Bearer <token>`（必须）
- Content-Type: `multipart/form-data`

### 请求字段（multipart）

1. `file`（必填）
- 类型：binary
- 含义：前端裁剪后的头像文件（推荐接收 `image/png`，兼容 `image/jpeg`）

### 成功响应（建议）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "WangNong_001",
    "userName": "王农",
    "gender": "男",
    "birthDate": "1990-01-01",
    "avatarSrc": "https://cdn.example.com/avatar/u_1_20260407.png",
    "role": "user"
  }
}
```

### 失败响应（建议）

```json
{
  "code": 400,
  "message": "invalid image file",
  "data": null
}
```

推荐错误场景：

1. 文件为空或字段缺失
2. 非图片 MIME
3. 文件过大（建议限制 5MB）
4. token 无效（401）
5. 存储失败（500）

---

## 4. 后端需确认/修改的字段契约

### 4.1 `UserProfile.avatarSrc`

当前前端读取 `profile.avatarSrc` 用于头像展示，要求：

1. 必须返回可直接访问的 URI（http/https 或可读本地 URI）
2. 建议返回最终 CDN URL，而不是内部临时路径
3. 更新头像后，`GET /api/v1/auth/profile` 也应返回新值

### 4.2 `POST /api/v1/traceability/scan` 返回数据

当前前端将 `data` 当作字符串展示，因此后端需要保持：

1. `data` 为字符串（或至少可安全转字符串）
2. 包含可直接给用户展示的作物溯源信息文本

---

## 5. 后端逻辑改造建议

### 5.1 头像上传处理链

建议最小处理流程：

1. 校验 token -> 定位用户
2. 校验 multipart `file`
3. 校验类型与大小
4. 存储文件（本地/对象存储）
5. 更新用户表 avatar 字段
6. 返回完整 `UserProfile`

### 5.2 幂等与缓存

1. 建议上传后覆盖旧头像并清理历史文件（或做版本化）
2. 头像 URL 建议带版本参数或文件名哈希，避免客户端缓存旧图

### 5.3 安全

1. 禁止执行文件内容
2. 仅允许白名单图片格式
3. 避免将临时路径直接暴露给外网

---

## 6. 联调验收清单

1. 拍照 -> 裁剪 -> 上传成功 -> 返回新 `avatarSrc` -> 个人页立即更新
2. 相册选图 -> 裁剪 -> 上传成功 -> 重新进入 App 仍显示新头像
3. 上传非法文件时返回明确错误信息
4. 摄像头扫码识别后，`/traceability/scan` 返回内容可展示
5. 相册识别二维码后，调用同一溯源接口返回一致结果

---

## 7. 同步文档位置

本次已同步：

1. `docs/asyncapi/asyncapi.yaml` 新增 `/api/v1/auth/avatar/upload`
2. `README.md` 更新页面、测试与 HTTP API 说明
