<#
.SYNOPSIS
    一键启动 万农智慧系统 (App ↔ Java ↔ Python) 全链路。

.DESCRIPTION
    本脚本按顺序启动:
      1. Python 模拟设备 (mock_device.py) — 向 MQTT 发布遥测数据
      2. Python 后端 (backend_main_debug.py) — MQTT 订阅 + SunAgent 调度
      3. Python Web (web_server_debug.py) — Flask API (AI 对话服务)
      4. Java HMHT 后端 — MQTT 桥接 + REST API

    按 Ctrl+C 或关闭窗口可终止所有子进程。

.PARAMETER Mode
    运行模式:
      mock   — Java 使用模拟数据 (默认, 不需要 Python)
      bridge — Java 桥接 Python, 启动全链路

.PARAMETER PythonDir
    Python agri 后端目录, 默认 d:\DevEcoStudioProjects\agri\agri_system\backend

.PARAMETER JavaDir
    Java HMHT 项目目录, 默认 d:\JetBrainsProjects\HMHT

.PARAMETER AgriChatPort
    Python 对话服务端口, 默认 5000

.EXAMPLE
    .\start-all.ps1 -Mode bridge
#>
param(
    [ValidateSet("mock", "bridge")]
    [string]$Mode = "mock",
    [string]$PythonDir = "d:\DevEcoStudioProjects\agri\agri_system\backend",
    [string]$JavaDir   = "d:\JetBrainsProjects\HMHT",
    [int]$AgriChatPort = 5000,
    [string]$InfluxDir = "C:\Users\mc_hi\Downloads\influxdb3-core-3.8.3-windows_amd64",
    [string]$InfluxToken = ""
)

$ErrorActionPreference = "Stop"
$jobs = @()

function Write-Banner([string]$text) {
    Write-Host ""
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host "  $text" -ForegroundColor Cyan
    Write-Host "======================================" -ForegroundColor Cyan
}

function Stop-AllJobs {
    Write-Host "" 
    Write-Host "[*] Stopping all services..." -ForegroundColor Yellow
    foreach ($j in $script:jobs) {
        try { Stop-Process -Id $j.Id -Force -ErrorAction SilentlyContinue } catch {}
    }
    Write-Host "[*] All services stopped." -ForegroundColor Green
}

# Register cleanup on exit
Register-EngineEvent PowerShell.Exiting -Action { Stop-AllJobs } | Out-Null

try {
    if ($Mode -eq "bridge") {
        # 0. InfluxDB (optional)
        $influxExe = Join-Path $InfluxDir "influxdb3.exe"
        if (Test-Path $influxExe) {
            Write-Banner "[0] InfluxDB 3 (optional)"
            $influxData = Join-Path $InfluxDir "data"
            $pInflux = Start-Process -FilePath $influxExe `
                -ArgumentList "serve", "--node-id", "local", "--object-store", "file", "--data-dir", $influxData `
                -WorkingDirectory $InfluxDir -PassThru
            $jobs += $pInflux
            Write-Host "    PID: $($pInflux.Id)" -ForegroundColor DarkGray
            Start-Sleep -Seconds 3

            if (-not $InfluxToken) {
                Write-Host "    Creating admin token..." -ForegroundColor DarkGray
                $tokenOut = & $influxExe create token --admin --host http://127.0.0.1:8181 --format json 2>&1
                try {
                    $tokenJson = $tokenOut | ConvertFrom-Json
                    $InfluxToken = $tokenJson.raw_token
                    if (-not $InfluxToken) { $InfluxToken = $tokenJson.token }
                    Write-Host "    Token acquired." -ForegroundColor Green
                } catch {
                    Write-Host "    Token creation returned: $tokenOut" -ForegroundColor Yellow
                    Write-Host "    InfluxDB may already have a token. Pass -InfluxToken <token> if so." -ForegroundColor Yellow
                }
            }
        } else {
            Write-Host "[0] InfluxDB not found at $InfluxDir (skipped)" -ForegroundColor DarkGray
        }

        # 1. Python mock_device
        Write-Banner "[1/4] Python mock_device.py"
        if (-not (Test-Path "$PythonDir\mock_device.py")) {
            Write-Warning "Not found: $PythonDir\mock_device.py  (skipped)"
        } else {
            $p1 = Start-Process -FilePath "cmd" -ArgumentList "/k", "python `"$PythonDir\mock_device.py`" & pause" `
                -WorkingDirectory $PythonDir -PassThru
            $jobs += $p1
            Write-Host "    PID: $($p1.Id)" -ForegroundColor DarkGray
            Start-Sleep -Seconds 2
        }

        # 2. Python backend (MQTT + SunAgent)
        Write-Banner "[2/4] Python backend_main_debug.py"
        if (-not (Test-Path "$PythonDir\backend_main_debug.py")) {
            Write-Warning "Not found: $PythonDir\backend_main_debug.py  (skipped)"
        } else {
            $p2 = Start-Process -FilePath "cmd" -ArgumentList "/k", "python `"$PythonDir\backend_main_debug.py`" & pause" `
                -WorkingDirectory $PythonDir -PassThru
            $jobs += $p2
            Write-Host "    PID: $($p2.Id)" -ForegroundColor DarkGray
            Start-Sleep -Seconds 3
        }

        # 3. Python web server (Flask API for AI chat on port $AgriChatPort)
        Write-Banner "[3/4] Python web_server_debug.py  (Flask :$AgriChatPort)"
        if (-not (Test-Path "$PythonDir\web_server_debug.py")) {
            Write-Warning "Not found: $PythonDir\web_server_debug.py  (skipped)"
        } else {
            $p3 = Start-Process -FilePath "cmd" -ArgumentList "/k", "python `"$PythonDir\web_server_debug.py`" & pause" `
                -WorkingDirectory $PythonDir -PassThru
            $jobs += $p3
            Write-Host "    PID: $($p3.Id)" -ForegroundColor DarkGray
            Write-Host "    Waiting for Flask to be ready..." -ForegroundColor DarkGray
            Start-Sleep -Seconds 3
        }
    }

    # 4. Java HMHT
    Write-Banner "[4/4] Java HMHT backend  (mode: $Mode)"

    $env:HMHT_RUN_MODE          = $Mode
    $env:HMHT_MOCK_SEED_ENABLED = "true"
    $env:HMHT_AGRI_CHAT_URL    = "http://localhost:$AgriChatPort"
    $env:HMHT_INFLUXDB_ENABLED  = if ($InfluxToken) { "true" } else { "false" }
    $env:HMHT_INFLUXDB_TOKEN    = $InfluxToken

    $gradlew = Join-Path $JavaDir "gradlew.bat"

    Write-Host "  Env vars:" -ForegroundColor DarkGray
    Write-Host "    HMHT_RUN_MODE          = $($env:HMHT_RUN_MODE)" -ForegroundColor DarkGray
    Write-Host "    HMHT_MOCK_SEED_ENABLED = $($env:HMHT_MOCK_SEED_ENABLED)" -ForegroundColor DarkGray
    Write-Host "    HMHT_AGRI_CHAT_URL    = $($env:HMHT_AGRI_CHAT_URL)" -ForegroundColor DarkGray
    Write-Host "    HMHT_INFLUXDB_ENABLED  = $($env:HMHT_INFLUXDB_ENABLED)" -ForegroundColor DarkGray
    Write-Host "    HMHT_INFLUXDB_TOKEN    = $(if ($env:HMHT_INFLUXDB_TOKEN) { '***' } else { '(empty)' })" -ForegroundColor DarkGray
    Write-Host ""

    Push-Location $JavaDir
    try {
        & $gradlew bootRun
    } finally {
        Pop-Location
    }

} finally {
    Stop-AllJobs
}
