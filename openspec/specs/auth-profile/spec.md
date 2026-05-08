# Auth Profile Specification

## Purpose

本规格记录当前认证、验证码、注册、找回密码、登录 token、个人资料和密码修改的已实现行为。它只描述现有代码，不引入新需求。

## Requirements

### Requirement: Verification Code

系统 SHALL 为手机号生成测试验证码并保存到数据库。

#### Scenario: Send Verification Code

- **WHEN** 客户端请求 `POST /api/v1/auth/send-verification-code` 并传入 `phone`
- **THEN** 系统生成 4 位数字验证码
- **AND** 系统将验证码保存到 `verification_codes`，过期时间为当前时间加 5 分钟，`used=false`
- **AND** 响应 `data.expires_in` 为 `300`，`data.test_code` 为生成的验证码

#### Scenario: Verify Latest Valid Code

- **WHEN** 注册或找回密码校验验证码
- **THEN** 系统只读取同手机号最近一条未使用且未过期验证码
- **AND** 验证成功后将该验证码标记为 `used=true`
- **AND** 没有可用验证码时返回业务错误 `400` / `验证码错误或已过期`
- **AND** 验证码内容不匹配时返回业务错误 `400` / `验证码错误`

### Requirement: Registration And Password Reset

系统 SHALL 使用验证码完成用户注册和密码重置。

#### Scenario: Register User

- **WHEN** 客户端请求 `POST /api/v1/auth/register` 并传入 `phone`、`verificationCode`、`password`
- **THEN** 系统先校验验证码
- **AND** 手机号已存在时返回业务错误 `409` / `该手机号已注册`
- **AND** 手机号不存在时创建用户，密码使用 BCrypt 编码，用户名为 `用户` 加手机号后 4 位，默认角色来自实体默认值 `user`
- **AND** 响应为空成功结果

#### Scenario: Reset Password

- **WHEN** 客户端请求 `POST /api/v1/auth/forgot-password` 并传入 `phone`、`verificationCode`、`newPassword`
- **THEN** 系统先校验验证码
- **AND** 用户不存在时返回业务错误 `404` / `用户不存在`
- **AND** 用户存在时用 BCrypt 编码后的新密码覆盖原密码
- **AND** 响应为空成功结果

### Requirement: Login Token

系统 SHALL 使用手机号和密码登录，并维护服务实例内的业务 token。

#### Scenario: Login Successfully

- **WHEN** 客户端请求 `POST /api/v1/auth/login` 并传入 `username` 和 `password`
- **THEN** 系统按 `username` 查询手机号并校验 BCrypt 密码
- **AND** 登录成功时生成 UUID token，保存到内存 `tokenStore`
- **AND** token 有效期为 `3600` 秒
- **AND** 系统写入 `LOGIN` 操作日志
- **AND** 响应 `data.token` 和 `data.expires_in`

#### Scenario: Login Fails

- **WHEN** 手机号不存在或密码不匹配
- **THEN** 系统返回业务错误 `401` / `用户名或密码错误`

#### Scenario: Require User

- **WHEN** 业务服务校验 token
- **THEN** token 不存在或已过期时返回业务错误 `401` / `未登录或 token 已过期`
- **AND** token 指向的用户不存在时返回业务错误 `401` / `用户不存在`
- **AND** 过期 token 会从内存中移除

#### Scenario: Logout

- **WHEN** 客户端请求 `POST /api/v1/auth/logout`
- **THEN** 系统从内存 tokenStore 移除该 token
- **AND** token 存在时写入 `LOGOUT` 操作日志
- **AND** 响应为空成功结果

### Requirement: Profile And Password

系统 SHALL 为已登录用户提供资料读取、资料更新和密码修改。

#### Scenario: Get Profile

- **WHEN** 客户端请求 `GET /api/v1/auth/profile` 并携带有效 token
- **THEN** 系统返回 `userId`、`userName`、`phone`、`gender`、`birthDate`、`avatarSrc`、`role`
- **AND** `userId` 格式为 `WangNong_` 加 3 位用户主键

#### Scenario: Update Profile

- **WHEN** 客户端请求 `PUT /api/v1/auth/profile` 并携带有效 token
- **THEN** 系统仅更新请求体中非 `null` 的 `userName`、`gender`、`birthDate`、`avatarSrc`
- **AND** `birthDate` 按 `LocalDate.parse` 解析
- **AND** 响应更新后的个人资料

#### Scenario: Change Password

- **WHEN** 客户端请求 `PUT /api/v1/auth/password` 并携带有效 token、`oldPassword`、`newPassword`
- **THEN** 系统先校验旧密码
- **AND** 旧密码不匹配时返回业务错误 `400` / `旧密码错误`
- **AND** 旧密码匹配时保存 BCrypt 编码后的新密码
- **AND** 响应为空成功结果

### Requirement: Admin Identity Boundary

系统 SHALL 通过业务 token 中的用户角色判断管理员身份。

#### Scenario: Require Admin

- **WHEN** 管理类 Service 调用 `requireAdmin`
- **THEN** 系统先按 token 获取当前用户
- **AND** 用户 `role` 不等于 `admin` 时返回业务错误 `403` / `需要管理员权限`
