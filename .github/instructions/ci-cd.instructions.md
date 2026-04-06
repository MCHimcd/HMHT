---
description: "Use when modifying GitHub Actions workflows, deployment pipeline, SSH secrets handling, bootJar build steps, or server restart flow in HMHT CI/CD."
applyTo: ".github/workflows/**,docs/deployment.md,build.gradle,settings.gradle,gradle.properties"
---

# CI/CD 与部署约定

## 适用范围

本文件用于规范 GitHub Actions 工作流与服务器部署链路。

## 当前部署基线

- 触发方式：`workflow_dispatch`
- 构建产物：Spring Boot 可运行 JAR
- 部署方式：SSH 上传 + systemd 重启

## 工作流修改规则

修改 `.github/workflows/deploy.yml` 时，需重点检查：

- JDK 版本与 `build.gradle` 的 toolchain 一致
- 构建命令是否符合目标（`bootJar` 是否需要带测试）
- 产物查找逻辑是否可定位可运行 JAR（排除 `*-plain.jar`）
- 远端路径（`APP_DIR`）与 systemd 的 `ExecStart` 一致
- 并发策略（`concurrency`）是否符合生产预期

## Secrets 与安全

必须通过 GitHub Secrets 注入敏感信息，不在仓库明文写入：

- `SERVER_HOST`
- `SERVER_PORT`
- `SERVER_USER`
- `SERVER_SSH_KEY`
- `APP_DIR`
- `SYSTEMD_SERVICE_NAME`
- `SERVER_KNOWN_HOST`（推荐）

修改 SSH 逻辑时，优先保留 known_hosts 校验，避免降低主机指纹验证强度。

## 部署文档同步

当 CI/CD 流程、Secrets、服务器重启命令或 systemd 依赖发生变化时，必须同步更新：

- `docs/deployment.md`
- README 的「部署」章节（若摘要描述受影响）

## 验证建议

- 语法校验：workflow YAML 结构合法
- 构建校验：工作流中的构建命令在本地可执行
- 部署校验：变更后至少完成一次手动触发部署演练
- 回滚校验：确认失败时可快速恢复上一版本 JAR

## 常见风险

- 升级 JDK 但未同步 toolchain，导致 CI 编译通过/运行失败
- 修改上传文件名但未同步 systemd `ExecStart`
- 关闭 known_hosts 校验导致中间人风险增加
- 在工作流中增加高耗时步骤却未调整并发和超时
