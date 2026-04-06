<#
.SYNOPSIS
    一键启动 万农智慧系统多模式链路。

.DESCRIPTION
    主控模式（默认）会在单个 Windows Terminal 窗口内创建多标签页，
    每个标签页由本脚本 Worker 模式托管一个服务进程。

    支持特性：
      1) 单窗口多标签页启动
      2) 主终端实时显示各标签页状态
      3) UTF-8 输出，避免中文乱码
      4) Ctrl+C 优雅停止，超时后强制回收

.PARAMETER Mode
    运行模式:
    mock     - App <-> Java (模拟数据)
    bridge   - App <-> Java <-> Python
    hardware - App <-> Java <-> 硬件端
    full     - App <-> Java <-> Python <-> 硬件端

.PARAMETER PythonDir
    Python agri 后端目录。

.PARAMETER JavaDir
    Java HMHT 项目目录。

.PARAMETER AgriChatPort
    Python 对话服务端口。

.PARAMETER InfluxDir
    InfluxDB 目录（包含 influxdb3.exe）。

.PARAMETER InfluxToken
    InfluxDB token。若为空且 Influx 已启动，则尝试自动创建。

.EXAMPLE
    .\start.ps1 -Mode bridge
#>
param(
    [ValidateSet("mock", "bridge", "hardware", "full")]
    [string]$Mode = "mock",
    [string]$PythonDir = "d:\DevEcoStudioProjects\agri\agri_system\backend",
    [string]$JavaDir = "d:\JetBrainsProjects\HMHT",
    [int]$AgriChatPort = 5000,
    [string]$InfluxDir = "C:\Users\mc_hi\Downloads\influxdb3-core-3.8.3-windows_amd64",
    [string]$InfluxToken = "",

    # Internal worker parameters. Do not set manually.
    [switch]$WorkerMode,
    [string]$WorkerName = "",
    [ValidateSet("python", "java", "influx")]
    [string]$WorkerKind = "python",
    [string]$WorkerWorkDir = "",
    [string]$WorkerEntry = "",
    [string]$RuntimeDir = "",
    [string]$StopSignalFile = "",
    [string]$WorkerHmhtMode = "mock",
    [int]$WorkerAgriChatPort = 5000,
    [string]$WorkerInfluxToken = ""
)

$ErrorActionPreference = "Stop"
$script:ShutdownStarted = $false

function Set-Utf8Console {
    $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
    [Console]::InputEncoding = $utf8NoBom
    [Console]::OutputEncoding = $utf8NoBom
    $OutputEncoding = $utf8NoBom
    try {
        chcp 65001 | Out-Null
    }
    catch {
        Write-Verbose "Failed to set code page to UTF-8: $($_.Exception.Message)"
    }
}

function Write-Banner {
    param([string]$Text)

    Write-Host ""
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host "  $Text" -ForegroundColor Cyan
    Write-Host "======================================" -ForegroundColor Cyan
}

function ConvertTo-SafeName {
    param([string]$Name)

    return ($Name -replace '[^a-zA-Z0-9_-]', '_')
}

function Get-WorkerStatusPath {
    param(
        [string]$BaseDir,
        [string]$Name
    )

    $safe = ConvertTo-SafeName -Name $Name
    return (Join-Path $BaseDir "$safe.json")
}

function Write-WorkerStatus {
    param(
        [string]$StatusFile,
        [string]$Name,
        [string]$Kind,
        [string]$State,
        [int]$ShellPid,
        [int]$ChildPid = 0,
        [int]$ExitCode = 0,
        [string]$Message = ""
    )

    $payload = [ordered]@{
        name = $Name
        kind = $Kind
        state = $State
        shellPid = $ShellPid
        childPid = $ChildPid
        exitCode = $ExitCode
        message = $Message
        updatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    }

    $payload | ConvertTo-Json -Depth 4 | Set-Content -Path $StatusFile -Encoding UTF8
}

function Read-ServiceStatus {
    param([object]$Service)

    if (-not (Test-Path $Service.StatusFile)) {
        return [pscustomobject]@{
            name = $Service.Name
            kind = $Service.Kind
            state = "Starting"
            shellPid = 0
            childPid = 0
            exitCode = 0
            message = "Waiting for worker status..."
            updatedAt = "-"
        }
    }

    try {
        $raw = Get-Content -Path $Service.StatusFile -Raw -Encoding UTF8
        if ([string]::IsNullOrWhiteSpace($raw)) {
            throw "Status file is empty"
        }
        return $raw | ConvertFrom-Json
    }
    catch {
        return [pscustomobject]@{
            name = $Service.Name
            kind = $Service.Kind
            state = "Unknown"
            shellPid = 0
            childPid = 0
            exitCode = 0
            message = "Status parse failed"
            updatedAt = "-"
        }
    }
}

function Stop-ProcessTreeGracefully {
    param(
        [int]$Pid,
        [int]$GraceSeconds = 8
    )

    if ($Pid -le 0) {
        return
    }

    $proc = Get-Process -Id $Pid -ErrorAction SilentlyContinue
    if ($null -eq $proc) {
        return
    }

    try {
        & taskkill /PID $Pid /T | Out-Null
    }
    catch {
        Write-Verbose "taskkill soft failed for PID ${Pid}: $($_.Exception.Message)"
    }

    $deadline = (Get-Date).AddSeconds($GraceSeconds)
    while ((Get-Date) -lt $deadline) {
        if ($null -eq (Get-Process -Id $Pid -ErrorAction SilentlyContinue)) {
            return
        }
        Start-Sleep -Milliseconds 400
    }

    try {
        & taskkill /PID $Pid /T /F | Out-Null
    }
    catch {
        Write-Verbose "taskkill force failed for PID ${Pid}: $($_.Exception.Message)"
    }
}

function Render-ServiceStatusTable {
    param(
        [object[]]$Services,
        [string]$Mode,
        [string]$WindowName,
        [string]$SessionDir,
        [string]$StopSignalFile
    )

    Clear-Host
    Write-Banner "HMHT start.ps1 - Service Status"
    Write-Host "Mode       : $Mode" -ForegroundColor DarkGray
    Write-Host "WT Window  : $WindowName" -ForegroundColor DarkGray
    Write-Host "Runtime Dir: $SessionDir" -ForegroundColor DarkGray
    Write-Host "Stop Signal: $StopSignalFile" -ForegroundColor DarkGray
    Write-Host ""

    $header = "{0,-3} {1,-22} {2,-10} {3,-8} {4,-19} {5}" -f "#", "Service", "State", "PID", "UpdatedAt", "Message"
    Write-Host $header -ForegroundColor Yellow
    Write-Host ("-" * 96) -ForegroundColor DarkGray

    $index = 1
    foreach ($svc in $Services) {
        $st = Read-ServiceStatus -Service $svc
        $pid = if ($st.childPid -and $st.childPid -gt 0) { [string]$st.childPid } else { "-" }
        $msg = if ([string]::IsNullOrWhiteSpace($st.message)) { "-" } else { $st.message }
        if ($msg.Length -gt 34) {
            $msg = $msg.Substring(0, 34) + "..."
        }

        $line = "{0,-3} {1,-22} {2,-10} {3,-8} {4,-19} {5}" -f $index, $svc.Name, $st.state, $pid, $st.updatedAt, $msg
        switch ($st.state) {
            "Running" { Write-Host $line -ForegroundColor Green }
            "Starting" { Write-Host $line -ForegroundColor Cyan }
            "Stopping" { Write-Host $line -ForegroundColor Yellow }
            "Stopped" { Write-Host $line -ForegroundColor DarkGray }
            "Failed" { Write-Host $line -ForegroundColor Red }
            default { Write-Host $line -ForegroundColor Magenta }
        }
        $index++
    }

    Write-Host ""
    Write-Host "按 Ctrl+C 触发统一停止：先优雅停止，超时后强制结束。" -ForegroundColor DarkYellow
}

function Start-ServiceTab {
    param(
        [object]$Service,
        [int]$Index,
        [string]$WindowName,
        [string]$ScriptPath,
        [string]$StatusDir,
        [string]$StopSignalFile,
        [string]$Mode,
        [int]$AgriChatPort,
        [string]$InfluxToken,
        [switch]$CaptureProcess
    )

    $title = "[$Index] $($Service.Name)"
    $tabArgs = @(
        "-w", $WindowName,
        "new-tab",
        "--title", $title,
        "-d", $Service.WorkDir,
        "powershell",
        "-NoLogo",
        "-ExecutionPolicy", "Bypass",
        "-File", $ScriptPath,
        "-WorkerMode",
        "-WorkerName", $Service.Name,
        "-WorkerKind", $Service.Kind,
        "-WorkerWorkDir", $Service.WorkDir,
        "-RuntimeDir", $StatusDir,
        "-StopSignalFile", $StopSignalFile,
        "-WorkerHmhtMode", $Mode,
        "-WorkerAgriChatPort", "$AgriChatPort",
        "-WorkerInfluxToken", $InfluxToken
    )

    if (-not [string]::IsNullOrWhiteSpace($Service.Entry)) {
        $tabArgs += @("-WorkerEntry", $Service.Entry)
    }

    if ($CaptureProcess) {
        return Start-Process -FilePath "wt" -ArgumentList $tabArgs -PassThru
    }

    Start-Process -FilePath "wt" -ArgumentList $tabArgs | Out-Null
    return $null
}

function Try-CreateInfluxToken {
    param(
        [string]$InfluxExe,
        [string]$CurrentToken
    )

    if (-not [string]::IsNullOrWhiteSpace($CurrentToken)) {
        return $CurrentToken
    }

    Write-Host "[*] 正在尝试创建 InfluxDB admin token..." -ForegroundColor DarkGray

    try {
        $tokenOut = & $InfluxExe create token --admin --host http://127.0.0.1:8181 --format json 2>&1
        $tokenJson = $tokenOut | ConvertFrom-Json

        $token = ""
        if ($tokenJson.raw_token) {
            $token = [string]$tokenJson.raw_token
        }
        elseif ($tokenJson.token) {
            $token = [string]$tokenJson.token
        }

        if ([string]::IsNullOrWhiteSpace($token)) {
            Write-Warning "Influx token 创建结果为空，请手动传入 -InfluxToken。"
            return ""
        }

        Write-Host "[*] InfluxDB token 获取成功。" -ForegroundColor Green
        return $token
    }
    catch {
        Write-Warning "Influx token 自动创建失败：$($_.Exception.Message)"
        return ""
    }
}

function Invoke-Worker {
    Set-Utf8Console

    if ([string]::IsNullOrWhiteSpace($WorkerName)) {
        throw "WorkerName is required in worker mode."
    }
    if ([string]::IsNullOrWhiteSpace($WorkerWorkDir)) {
        throw "WorkerWorkDir is required in worker mode."
    }
    if ([string]::IsNullOrWhiteSpace($RuntimeDir)) {
        throw "RuntimeDir is required in worker mode."
    }

    if (-not (Test-Path $RuntimeDir)) {
        New-Item -ItemType Directory -Path $RuntimeDir -Force | Out-Null
    }

    $statusFile = Get-WorkerStatusPath -BaseDir $RuntimeDir -Name $WorkerName
    $childProc = $null
    $message = ""

    Write-WorkerStatus -StatusFile $statusFile -Name $WorkerName -Kind $WorkerKind -State "Starting" -ShellPid $PID -Message "Worker bootstrap"

    try {
        switch ($WorkerKind) {
            "python" {
                if ([string]::IsNullOrWhiteSpace($WorkerEntry)) {
                    throw "WorkerEntry is required for python worker."
                }
                $scriptPath = Join-Path $WorkerWorkDir $WorkerEntry
                if (-not (Test-Path $scriptPath)) {
                    throw "Not found: $scriptPath"
                }

                $env:PYTHONUTF8 = "1"
                $env:PYTHONIOENCODING = "utf-8"
                $childProc = Start-Process -FilePath "python" -ArgumentList @("-X", "utf8", $WorkerEntry) -WorkingDirectory $WorkerWorkDir -NoNewWindow -PassThru
                $message = "python $WorkerEntry"
            }
            "java" {
                $gradlew = Join-Path $WorkerWorkDir "gradlew.bat"
                if (-not (Test-Path $gradlew)) {
                    throw "Not found: $gradlew"
                }

                $env:HMHT_RUN_MODE = $WorkerHmhtMode
                $env:HMHT_MOCK_SEED_ENABLED = "true"
                $env:HMHT_AGRI_CHAT_URL = "http://localhost:$WorkerAgriChatPort"
                if ([string]::IsNullOrWhiteSpace($WorkerInfluxToken)) {
                    $env:HMHT_INFLUXDB_ENABLED = "false"
                    $env:HMHT_INFLUXDB_TOKEN = ""
                }
                else {
                    $env:HMHT_INFLUXDB_ENABLED = "true"
                    $env:HMHT_INFLUXDB_TOKEN = $WorkerInfluxToken
                }

                $env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
                $childProc = Start-Process -FilePath $gradlew -ArgumentList @("--no-daemon", "bootRun") -WorkingDirectory $WorkerWorkDir -NoNewWindow -PassThru
                $message = "gradlew.bat --no-daemon bootRun"
            }
            "influx" {
                $influxExe = Join-Path $WorkerWorkDir "influxdb3.exe"
                if (-not (Test-Path $influxExe)) {
                    throw "Not found: $influxExe"
                }

                $influxData = Join-Path $WorkerWorkDir "data"
                $childProc = Start-Process -FilePath $influxExe -ArgumentList @("serve", "--node-id", "local", "--object-store", "file", "--data-dir", $influxData) -WorkingDirectory $WorkerWorkDir -NoNewWindow -PassThru
                $message = "influxdb3 serve"
            }
        }

        Write-WorkerStatus -StatusFile $statusFile -Name $WorkerName -Kind $WorkerKind -State "Running" -ShellPid $PID -ChildPid $childProc.Id -Message $message

        while ($true) {
            if ($childProc.HasExited) {
                break
            }

            if (-not [string]::IsNullOrWhiteSpace($StopSignalFile) -and (Test-Path $StopSignalFile)) {
                Write-WorkerStatus -StatusFile $statusFile -Name $WorkerName -Kind $WorkerKind -State "Stopping" -ShellPid $PID -ChildPid $childProc.Id -Message "Stop signal received"
                Stop-ProcessTreeGracefully -Pid $childProc.Id -GraceSeconds 8
                break
            }

            Write-WorkerStatus -StatusFile $statusFile -Name $WorkerName -Kind $WorkerKind -State "Running" -ShellPid $PID -ChildPid $childProc.Id -Message $message
            Wait-Process -Id $childProc.Id -Timeout 1 -ErrorAction SilentlyContinue
        }

        if (-not $childProc.HasExited) {
            Wait-Process -Id $childProc.Id -Timeout 2 -ErrorAction SilentlyContinue
        }

        $exitCode = if ($childProc.HasExited) { $childProc.ExitCode } else { 1 }
        $endState = if ($exitCode -eq 0) { "Stopped" } else { "Failed" }

        Write-WorkerStatus -StatusFile $statusFile -Name $WorkerName -Kind $WorkerKind -State $endState -ShellPid $PID -ChildPid $childProc.Id -ExitCode $exitCode -Message $message
        return $exitCode
    }
    catch {
        Write-WorkerStatus -StatusFile $statusFile -Name $WorkerName -Kind $WorkerKind -State "Failed" -ShellPid $PID -ChildPid 0 -ExitCode 1 -Message $_.Exception.Message
        throw
    }
    finally {
        if ($childProc -and -not $childProc.HasExited) {
            Stop-ProcessTreeGracefully -Pid $childProc.Id -GraceSeconds 3
        }
    }
}

if ($WorkerMode) {
    exit (Invoke-Worker)
}

Set-Utf8Console

$wtCommand = Get-Command "wt" -ErrorAction SilentlyContinue
if ($null -eq $wtCommand) {
    throw "未检测到 Windows Terminal (wt.exe)。请安装后使用 start.ps1。"
}

if (-not (Test-Path $JavaDir)) {
    throw "JavaDir not found: $JavaDir"
}

$sessionId = (Get-Date -Format "yyyyMMdd-HHmmss") + "-$PID"
$sessionDir = Join-Path (Join-Path $JavaDir ".start-runtime") $sessionId
$statusDir = Join-Path $sessionDir "status"
$stopSignalFile = Join-Path $sessionDir "stop.signal"
$windowName = "hmht-start-$sessionId"

New-Item -ItemType Directory -Path $statusDir -Force | Out-Null

$services = New-Object System.Collections.Generic.List[object]

$influxExe = Join-Path $InfluxDir "influxdb3.exe"
if (Test-Path $influxExe) {
    $services.Add([pscustomobject]@{
        Name = "InfluxDB"
        Kind = "influx"
        WorkDir = $InfluxDir
        Entry = ""
        StatusFile = Get-WorkerStatusPath -BaseDir $statusDir -Name "InfluxDB"
    })
}
else {
    Write-Host "[0] InfluxDB not found at $InfluxDir (skipped)" -ForegroundColor DarkGray
}

if ($Mode -in @("bridge", "full")) {
    $pythonScripts = @(
        [pscustomobject]@{ Name = "Mock Device"; Entry = "mock_device.py" },
        [pscustomobject]@{ Name = "Backend"; Entry = "backend_main_debug.py" },
        [pscustomobject]@{ Name = "Flask"; Entry = "web_server_debug.py" }
    )

    foreach ($item in $pythonScripts) {
        $filePath = Join-Path $PythonDir $item.Entry
        if (-not (Test-Path $filePath)) {
            Write-Warning "Not found: $filePath (skipped)"
            continue
        }

        $services.Add([pscustomobject]@{
            Name = $item.Name
            Kind = "python"
            WorkDir = $PythonDir
            Entry = $item.Entry
            StatusFile = Get-WorkerStatusPath -BaseDir $statusDir -Name $item.Name
        })
    }
}

$services.Add([pscustomobject]@{
    Name = "Java HMHT"
    Kind = "java"
    WorkDir = $JavaDir
    Entry = ""
    StatusFile = Get-WorkerStatusPath -BaseDir $statusDir -Name "Java HMHT"
})

if ($services.Count -eq 0) {
    throw "No services to start."
}

Write-Banner "启动服务: 单窗口多标签"
Write-Host "Mode: $Mode" -ForegroundColor DarkGray
Write-Host "WT window: $windowName" -ForegroundColor DarkGray
Write-Host ""

$wtOwnerProcess = $null
$index = 1

foreach ($svc in $services) {
    $capture = $index -eq 1
    $proc = Start-ServiceTab -Service $svc -Index $index -WindowName $windowName -ScriptPath $PSCommandPath -StatusDir $statusDir -StopSignalFile $stopSignalFile -Mode $Mode -AgriChatPort $AgriChatPort -InfluxToken $InfluxToken -CaptureProcess:$capture
    if ($capture) {
        $wtOwnerProcess = $proc
    }

    if ($svc.Kind -eq "influx") {
        Start-Sleep -Seconds 3
        if ([string]::IsNullOrWhiteSpace($InfluxToken)) {
            $InfluxToken = Try-CreateInfluxToken -InfluxExe $influxExe -CurrentToken $InfluxToken
        }
    }

    Start-Sleep -Milliseconds 400
    $index++
}

function Invoke-OrchestratorShutdown {
    param(
        [object[]]$Services,
        [string]$StopSignalFile,
        [string]$WindowName,
        [object]$WtOwnerProcess,
        [string]$Reason
    )

    if ($script:ShutdownStarted) {
        return
    }
    $script:ShutdownStarted = $true

    Write-Host ""
    Write-Host "[*] 停止中: $Reason" -ForegroundColor Yellow

    if (-not (Test-Path $StopSignalFile)) {
        New-Item -ItemType File -Path $StopSignalFile -Force | Out-Null
    }

    $deadline = (Get-Date).AddSeconds(12)
    while ((Get-Date) -lt $deadline) {
        $allStopped = $true
        foreach ($svc in $Services) {
            $st = Read-ServiceStatus -Service $svc
            if ($st.state -in @("Starting", "Running", "Stopping", "Unknown")) {
                $allStopped = $false
                break
            }
        }

        if ($allStopped) {
            break
        }
        Start-Sleep -Milliseconds 500
    }

    foreach ($svc in $Services) {
        $st = Read-ServiceStatus -Service $svc
        if ($st.childPid -and $st.childPid -gt 0) {
            Stop-ProcessTreeGracefully -Pid ([int]$st.childPid) -GraceSeconds 2
        }
        if ($st.shellPid -and $st.shellPid -gt 0) {
            Stop-ProcessTreeGracefully -Pid ([int]$st.shellPid) -GraceSeconds 2
        }
    }

    if ($WtOwnerProcess -and -not $WtOwnerProcess.HasExited) {
        try {
            $null = $WtOwnerProcess.CloseMainWindow()
        }
        catch {
            Write-Verbose "CloseMainWindow failed: $($_.Exception.Message)"
        }

        Start-Sleep -Milliseconds 800
        if (-not $WtOwnerProcess.HasExited) {
            Stop-ProcessTreeGracefully -Pid $WtOwnerProcess.Id -GraceSeconds 2
        }
    }

    try {
        $wtByCmd = Get-CimInstance Win32_Process -Filter "Name='WindowsTerminal.exe'" | Where-Object {
            $_.CommandLine -like "*$WindowName*"
        }
        foreach ($item in $wtByCmd) {
            Stop-ProcessTreeGracefully -Pid $item.ProcessId -GraceSeconds 2
        }
    }
    catch {
        Write-Verbose "Failed to query WindowsTerminal process: $($_.Exception.Message)"
    }

    Write-Host "[*] 所有服务已停止。" -ForegroundColor Green
}

try {
    while ($true) {
        Render-ServiceStatusTable -Services $services -Mode $Mode -WindowName $windowName -SessionDir $sessionDir -StopSignalFile $stopSignalFile

        $allDone = $true
        foreach ($svc in $services) {
            $st = Read-ServiceStatus -Service $svc
            if ($st.state -in @("Starting", "Running", "Stopping", "Unknown")) {
                $allDone = $false
                break
            }
        }

        if ($allDone) {
            Write-Host ""
            Write-Host "[*] 所有标签页任务已结束。" -ForegroundColor DarkGray
            break
        }

        Start-Sleep -Seconds 2
    }
}
finally {
    Invoke-OrchestratorShutdown -Services $services -StopSignalFile $stopSignalFile -WindowName $windowName -WtOwnerProcess $wtOwnerProcess -Reason "主终端退出或 Ctrl+C"
}
