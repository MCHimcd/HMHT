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
    Write-Host '[*] Stopping all services...' -ForegroundColor Yellow
    foreach ($j in $script:jobs) {
        try { Stop-Process -Id $j.Id -Force -ErrorAction SilentlyContinue } catch {}
    }
    Write-Host '[*] All services stopped.' -ForegroundColor Green
}

# Register cleanup on exit
Register-EngineEvent PowerShell.Exiting -Action { Stop-AllJobs } | Out-Null

try {
    # 检测 Windows Terminal (wt.exe) 是否可用
    $wtAvailable = $null -ne (Get-Command "wt" -ErrorAction SilentlyContinue)
    if ($wtAvailable) {
        Write-Host '[*] 检测到 Windows Terminal, 各服务将在同一窗口的不同标签页启动' -ForegroundColor DarkCyan
    } else {
        Write-Host '[*] 未检测到 Windows Terminal, 回退到独立窗口模式' -ForegroundColor DarkYellow
    }

    # WT 标签页参数列表
    $wtTabs = [System.Collections.Generic.List[string]]::new()
    $tabDelay = 0

    if ($Mode -eq "bridge") {
        # 0. InfluxDB (optional)
        $influxExe = Join-Path $InfluxDir "influxdb3.exe"
        if (Test-Path $influxExe) {
            Write-Banner '[0] InfluxDB 3 (optional)'
            $influxData = Join-Path $InfluxDir "data"
            $pInflux = Start-Process -FilePath $influxExe `
                -ArgumentList "serve", "--node-id", "local", "--object-store", "file", "--data-dir", $influxData `
                -WorkingDirectory $InfluxDir -PassThru
            $jobs += $pInflux
            Write-Host "    PID: $($pInflux.Id)" -ForegroundColor DarkGray
            Start-Sleep -Seconds 3

            if (-not $InfluxToken) {
                Write-Host '    Creating admin token...' -ForegroundColor DarkGray
                $tokenOut = & $influxExe create token --admin --host http://127.0.0.1:8181 --format json 2>&1
                try {
                    $tokenJson = $tokenOut | ConvertFrom-Json
                    $InfluxToken = $tokenJson.raw_token
                    if (-not $InfluxToken) { $InfluxToken = $tokenJson.token }
                    Write-Host '    Token acquired.' -ForegroundColor Green
                } catch {
                    Write-Host "    Token creation returned: $tokenOut" -ForegroundColor Yellow
                    Write-Host '    InfluxDB may already have a token. Pass -InfluxToken if so.' -ForegroundColor Yellow
                }
            }
        } else {
            Write-Host "[0] InfluxDB not found at $InfluxDir (skipped)" -ForegroundColor DarkGray
        }

        if ($wtAvailable) {
            # --- Windows Terminal 多标签页模式 ---
            Write-Banner '[1-3/4] Python 服务 -> WT 标签页'

            if (Test-Path "$PythonDir\mock_device.py") {
                if ($tabDelay -gt 0) {
                    $cmd = 'timeout /t ' + $tabDelay + ' /nobreak >nul & python mock_device.py'
                } else {
                    $cmd = 'python mock_device.py'
                }
                $wtTabs.Add('new-tab --title "[1] Mock Device" -d "' + $PythonDir + '" -- cmd /k "' + $cmd + '"')
                $tabDelay += 3
            } else { Write-Warning "Not found: $PythonDir\mock_device.py (skipped)" }

            if (Test-Path "$PythonDir\backend_main_debug.py") {
                $cmd = 'timeout /t ' + $tabDelay + ' /nobreak >nul & python backend_main_debug.py'
                $wtTabs.Add('new-tab --title "[2] Backend" -d "' + $PythonDir + '" -- cmd /k "' + $cmd + '"')
                $tabDelay += 3
            } else { Write-Warning "Not found: $PythonDir\backend_main_debug.py (skipped)" }

            if (Test-Path "$PythonDir\web_server_debug.py") {
                $cmd = 'timeout /t ' + $tabDelay + ' /nobreak >nul & python web_server_debug.py'
                $wtTabs.Add('new-tab --title "[3] Flask :' + $AgriChatPort + '" -d "' + $PythonDir + '" -- cmd /k "' + $cmd + '"')
                $tabDelay += 3
            } else { Write-Warning "Not found: $PythonDir\web_server_debug.py (skipped)" }

        } else {
            # --- 回退: 独立 cmd 窗口 ---
            Write-Banner '[1/4] Python mock_device.py'
            if (-not (Test-Path "$PythonDir\mock_device.py")) {
                Write-Warning "Not found: $PythonDir\mock_device.py  (skipped)"
            } else {
                $cmdArgs = '/k "python "' + $PythonDir + '\mock_device.py""'
                $p1 = Start-Process -FilePath "cmd" -ArgumentList $cmdArgs `
                    -WorkingDirectory $PythonDir -PassThru
                $jobs += $p1
                Write-Host "    PID: $($p1.Id)" -ForegroundColor DarkGray
                Start-Sleep -Seconds 2
            }

            Write-Banner '[2/4] Python backend_main_debug.py'
            if (-not (Test-Path "$PythonDir\backend_main_debug.py")) {
                Write-Warning "Not found: $PythonDir\backend_main_debug.py  (skipped)"
            } else {
                $cmdArgs = '/k "python "' + $PythonDir + '\backend_main_debug.py""'
                $p2 = Start-Process -FilePath "cmd" -ArgumentList $cmdArgs `
                    -WorkingDirectory $PythonDir -PassThru
                $jobs += $p2
                Write-Host "    PID: $($p2.Id)" -ForegroundColor DarkGray
                Start-Sleep -Seconds 3
            }

            Write-Banner '[3/4] Python web_server_debug.py  (Flask :' + $AgriChatPort + ')'
            if (-not (Test-Path "$PythonDir\web_server_debug.py")) {
                Write-Warning "Not found: $PythonDir\web_server_debug.py  (skipped)"
            } else {
                $cmdArgs = '/k "python "' + $PythonDir + '\web_server_debug.py""'
                $p3 = Start-Process -FilePath "cmd" -ArgumentList $cmdArgs `
                    -WorkingDirectory $PythonDir -PassThru
                $jobs += $p3
                Write-Host "    PID: $($p3.Id)" -ForegroundColor DarkGray
                Write-Host '    Waiting for Flask to be ready...' -ForegroundColor DarkGray
                Start-Sleep -Seconds 3
            }
        }
    }

    # 4. Java HMHT
    Write-Banner "[4/4] Java HMHT backend  (mode: $Mode)"
    $gradlew = Join-Path $JavaDir "gradlew.bat"
    $influxEnabled = if ($InfluxToken) { "true" } else { "false" }

    Write-Host "  Env vars:" -ForegroundColor DarkGray
    Write-Host "    HMHT_RUN_MODE          = $Mode" -ForegroundColor DarkGray
    Write-Host '    HMHT_MOCK_SEED_ENABLED = true' -ForegroundColor DarkGray
    Write-Host "    HMHT_AGRI_CHAT_URL     = http://localhost:$AgriChatPort" -ForegroundColor DarkGray
    Write-Host "    HMHT_INFLUXDB_ENABLED  = $influxEnabled" -ForegroundColor DarkGray
    Write-Host "    HMHT_INFLUXDB_TOKEN    = $(if ($InfluxToken) { '***' } else { '(empty)' })" -ForegroundColor DarkGray
    Write-Host ""

    if ($wtAvailable) {
        # Java 标签页: 通过 cmd set 传递环境变量
        $setEnv  = 'set HMHT_RUN_MODE=' + $Mode
        $setEnv += '& set HMHT_MOCK_SEED_ENABLED=true'
        $setEnv += '& set HMHT_AGRI_CHAT_URL=http://localhost:' + $AgriChatPort
        if ($InfluxToken) {
            $setEnv += '& set HMHT_INFLUXDB_ENABLED=true'
            $setEnv += '& set HMHT_INFLUXDB_TOKEN=' + $InfluxToken
        } else {
            $setEnv += '& set HMHT_INFLUXDB_ENABLED=false'
        }
        if ($tabDelay -gt 0) {
            $setEnv += '& timeout /t ' + $tabDelay + ' /nobreak >nul'
        }
        $setEnv += '& gradlew.bat bootRun'

        $wtTabs.Add('new-tab --title "[4] Java HMHT" -d "' + $JavaDir + '" -- cmd /k "' + $setEnv + '"')

        # 一次性启动全部标签页
        $wtArgs = $wtTabs -join ' ; '
        Write-Host "  启动 Windows Terminal ($($wtTabs.Count) 个标签页)..." -ForegroundColor Cyan
        Start-Process "wt" -ArgumentList $wtArgs

        if ($jobs.Count -gt 0) {
            Write-Host ""
            Write-Host "  InfluxDB 在后台运行 (PID: $($jobs[0].Id)), 按 Ctrl+C 可停止" -ForegroundColor Yellow
            try { while ($true) { Start-Sleep -Seconds 5 } } catch {}
        } else {
            Write-Host '  所有服务已在 Windows Terminal 标签页中启动, 此窗口可关闭' -ForegroundColor Green
        }
    } else {
        # 回退: Java 在当前窗口阻塞运行
        $env:HMHT_RUN_MODE          = $Mode
        $env:HMHT_MOCK_SEED_ENABLED = "true"
        $env:HMHT_AGRI_CHAT_URL     = "http://localhost:$AgriChatPort"
        $env:HMHT_INFLUXDB_ENABLED  = $influxEnabled
        $env:HMHT_INFLUXDB_TOKEN    = $InfluxToken

        Push-Location $JavaDir
        try {
            & $gradlew bootRun
        } finally {
            Pop-Location
        }
    }

} finally {
    Stop-AllJobs
}