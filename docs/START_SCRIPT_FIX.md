# scripts/start.ps1 修复说明

## 问题描述

`scripts/start.ps1` 脚本出现了 PowerShell 解析错误，主要症状：
- 多处"表达式或语句中包含意外的标记"}"错误
- 数组索引表达式丢失或无效
- 中文字符显示为乱码（如"鑾峰彇鎴愬姛"而不是"获取成功"）

## 根本原因

**文件编码问题**：脚本文件保存时使用了错误的编码格式，导致 PowerShell 解析器无法正确识别中文字符，进而引发语法错误。

## 修复方案

将 `scripts/start.ps1` 重新保存为 **UTF-8 with BOM** 编码：

```powershell
$content = Get-Content "scripts/start.ps1" -Raw -Encoding UTF8
$utf8BOM = New-Object System.Text.UTF8Encoding $true
[System.IO.File]::WriteAllText("scripts/start.ps1", $content, $utf8BOM)
```

## 验证结果

### 1. 语法检查
```
✓ PowerShell 解析器：0 个语法错误
✓ 文件编码：UTF-8 with BOM (EF BB BF)
```

### 2. 功能测试
创建了完整的测试套件 `scripts/tests/start.ps1.test.ps1`，所有测试通过：

- [x] 语法检查
- [x] 帮助文档
- [x] Mode 参数验证
- [x] Worker 模式参数
- [x] Windows Terminal 检测
- [x] Java 环境检测
- [x] 项目路径验证
- [x] UTF-8 编码检查
- [x] 关键函数定义
- [x] 模式配置测试

结果：**10/10 测试通过**

## 使用方法

### 基本用法

```powershell
# Mock 模式（默认）- 仅 App + Java
.\scripts\start.ps1

# Bridge 模式 - App + Java + Python
.\scripts\start.ps1 -Mode bridge

# Hardware 模式 - App + Java + 硬件
.\scripts\start.ps1 -Mode hardware

# Full 模式 - App + Java + Python + 硬件
.\scripts\start.ps1 -Mode full
```

### 自定义路径

```powershell
.\scripts\start.ps1 -Mode bridge `
    -PythonDir "D:\Projects\agri\backend" `
    -JavaDir "D:\Projects\HMHT" `
    -InfluxDir "D:\Tools\influxdb"
```

### 指定 InfluxDB Token

```powershell
.\scripts\start.ps1 -InfluxToken "your-influx-token-here"
```

## 前置要求

1. **Windows Terminal** - 必须安装（用于多标签页管理）
2. **Java 21+** - 需要设置 JAVA_HOME 环境变量
3. **InfluxDB 3.x** - 用于时序数据存储
4. **Python 3.x** - 仅 bridge/full 模式需要
5. **Gradle** - Java 项目已包含 gradlew.bat

## 测试方法

运行测试套件验证脚本功能：

```powershell
.\scripts\tests\start.ps1.test.ps1
```

预期输出：
```
====================================
  start.ps1 Test Suite
====================================
...
====================================
Test Complete
====================================
Passed: 10
Failed: 0

All tests passed!
```

## 架构说明

### 主控模式（默认）
- 启动 Windows Terminal 窗口
- 为每个服务创建独立标签页
- 实时监控服务状态
- 支持 Ctrl+C 优雅停止

### Worker 模式（内部）
- 由主控自动调用
- 每个标签页运行一个 Worker
- Worker 负责启动和监控单个服务
- 自动更新状态文件

### 支持的服务类型
- **InfluxDB** (`influx`) - 时序数据库
- **Java HMHT** (`java`) - Spring Boot 后端
- **Python Agri** (`python`) - AI/视觉服务

## 运行模式详解

| 模式 | InfluxDB | Java | Python | 硬件 | 说明 |
|------|----------|------|--------|------|------|
| mock | ✓ | ✓ | - | - | 模拟数据，快速开发 |
| bridge | ✓ | ✓ | ✓ | - | 连接 Python AI 服务 |
| hardware | ✓ | ✓ | - | ✓ | 连接真实硬件设备 |
| full | ✓ | ✓ | ✓ | ✓ | 完整链路（生产环境） |

## 故障排查

### 1. 编码错误再次出现
```powershell
# 重新保存为 UTF-8 BOM
$content = Get-Content "start.ps1" -Raw -Encoding UTF8
$utf8BOM = New-Object System.Text.UTF8Encoding $true
[System.IO.File]::WriteAllText("start.ps1", $content, $utf8BOM)
```

### 2. Windows Terminal 未找到
```bash
# 安装 Windows Terminal
winget install Microsoft.WindowsTerminal
```

### 3. Java 环境未配置
```powershell
# 检查 JAVA_HOME
echo $env:JAVA_HOME

# 设置 JAVA_HOME（临时）
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```

### 4. InfluxDB 启动失败
- 检查端口 8086 是否被占用
- 确认 InfluxDB 目录路径正确
- 查看 InfluxDB 日志文件

## 维护建议

1. **编辑器配置**：使用 UTF-8 with BOM 编码保存 PowerShell 脚本
2. **版本控制**：.gitattributes 中添加 `*.ps1 text eol=crlf`
3. **定期测试**：每次修改后运行 `scripts/tests/start.ps1.test.ps1`
4. **文档同步**：更新功能时同步更新本文档

## 修复记录

- **日期**：2026-04-07
- **问题**：PowerShell 语法错误（编码问题）
- **修复**：重新保存为 UTF-8 with BOM
- **验证**：10/10 测试通过
- **状态**：✓ 已修复并验证
