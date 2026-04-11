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
    [string]$Mode = "full",
    [string]$PythonDir = "d:\projects\ww\agri\agri_system\backend",
    [string]$JavaDir = "",
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

function ConvertTo-CommandLineArgumentString {
    param([string[]]$Arguments)

    $result = New-Object System.Collections.Generic.List[string]
    foreach ($arg in $Arguments) {
        if ($null -eq $arg -or $arg.Length -eq 0) {
            $result.Add('""')
            continue
        }

        if ($arg -notmatch '[\s"]') {
            $result.Add($arg)
            continue
        }

        $escaped = $arg.Replace('"', '\"')
        $result.Add('"' + $escaped + '"')
    }

    return ($result -join ' ')
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

    $json = $payload | ConvertTo-Json -Depth 4
    $tmpFile = $StatusFile + ".tmp"
    try {
        $json | Set-Content -Path $tmpFile -Encoding UTF8
        Move-Item -Path $tmpFile -Destination $StatusFile -Force
    }
    catch {
        # 竞态写入失败时静默忽略，下次循环会重试
        Remove-Item -Path $tmpFile -Force -ErrorAction SilentlyContinue
    }
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
        [int]$ProcessId,
        [int]$GraceSeconds = 8
    )

    if ($ProcessId -le 0) {
        return
    }

    $proc = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if ($null -eq $proc) {
        return
    }

    try {
        & taskkill /PID $ProcessId /T 2>&1 | Out-Null
    }
    catch {
        Write-Verbose "taskkill soft failed for PID ${ProcessId}: $($_.Exception.Message)"
    }

    $deadline = (Get-Date).AddSeconds($GraceSeconds)
    while ((Get-Date) -lt $deadline) {
        if ($null -eq (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
            return
        }
        Start-Sleep -Milliseconds 400
    }

    try {
        & taskkill /PID $ProcessId /T /F 2>&1 | Out-Null
    }
    catch {
        Write-Verbose "taskkill force failed for PID ${ProcessId}: $($_.Exception.Message)"
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
        $displayPid = if ($st.childPid -and $st.childPid -gt 0) { [string]$st.childPid } else { "-" }
        $msg = if ([string]::IsNullOrWhiteSpace($st.message)) { "-" } else { $st.message }
        if ($msg.Length -gt 34) {
            $msg = $msg.Substring(0, 34) + "..."
        }

        $line = "{0,-3} {1,-22} {2,-10} {3,-8} {4,-19} {5}" -f $index, $svc.Name, $st.state, $displayPid, $st.updatedAt, $msg
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
        "-WorkerAgriChatPort", "$AgriChatPort"
    )

    if (-not [string]::IsNullOrWhiteSpace($Service.Entry)) {
        $tabArgs += @("-WorkerEntry", $Service.Entry)
    }

    if (-not [string]::IsNullOrWhiteSpace($InfluxToken)) {
        $tabArgs += @("-WorkerInfluxToken", $InfluxToken)
    }

    $tabArgumentLine = ConvertTo-CommandLineArgumentString -Arguments $tabArgs

    if ($CaptureProcess) {
        return Start-Process -FilePath "wt" -ArgumentList $tabArgumentLine -PassThru
    }

    Start-Process -FilePath "wt" -ArgumentList $tabArgumentLine | Out-Null
    return $null
}

function Try-CreateInfluxToken {
    param(
        [string]$InfluxExe,
        [string]$CurrentToken
    )

    if (-not [string]::IsNullOrWhiteSpace($CurrentToken)) {
        return [pscustomobject]@{
            token = $CurrentToken
            alreadyExists = $false
            errorMessage = ""
        }
    }

    Write-Host "[*] 正在尝试创建 InfluxDB admin token..." -ForegroundColor DarkGray

    try {
        $tokenOut = & $InfluxExe create token --admin --host http://127.0.0.1:8181 --format json 2>&1
        
        # 将全部输出合并为字符串，统一检测错误（stdout/stderr 均覆盖）
        $rawText = ($tokenOut | Out-String).Trim()

        if ($rawText -match "token name already exists") {
            Write-Host "[*] InfluxDB admin token 已存在，跳过创建。" -ForegroundColor DarkGray
            return [pscustomobject]@{
                token = ""
                alreadyExists = $true
                errorMessage = "token name already exists"
            }
        }

        $tokenJson = $rawText | ConvertFrom-Json

        $token = ""
        if ($tokenJson.raw_token) {
            $token = [string]$tokenJson.raw_token
        }
        elseif ($tokenJson.token) {
            $token = [string]$tokenJson.token
        }

        if ([string]::IsNullOrWhiteSpace($token)) {
            Write-Warning "Influx token 创建结果为空，请手动传入 -InfluxToken。"
            return [pscustomobject]@{
                token = ""
                alreadyExists = $false
                errorMessage = "empty token response"
            }
        }

        Write-Host "[*] InfluxDB token 获取成功。" -ForegroundColor Green
        return [pscustomobject]@{
            token = $token
            alreadyExists = $false
            errorMessage = ""
        }
    }
    catch {
        $errorMsg = $_.Exception.Message
        if ($errorMsg -match "token name already exists") {
            Write-Host "[*] InfluxDB admin token 已存在，跳过创建。" -ForegroundColor DarkGray
            return [pscustomobject]@{
                token = ""
                alreadyExists = $true
                errorMessage = "token name already exists"
            }
        }
        Write-Warning "Influx token 自动创建失败：$errorMsg"
        return [pscustomobject]@{
            token = ""
            alreadyExists = $false
            errorMessage = $errorMsg
        }
    }
}

function Resolve-InfluxToken {
    param(
        [string]$InfluxExe,
        [string]$InfluxDir,
        [string]$CurrentToken,
        [int]$MaxAttempts = 5,
        [int]$RetrySeconds = 2
    )

    $tokenDir = Join-Path $InfluxDir "data"
    $tokenFile = Join-Path $tokenDir "hmht-admin.token"

    if (-not [string]::IsNullOrWhiteSpace($CurrentToken)) {
        try {
            if (-not (Test-Path $tokenDir)) {
                New-Item -ItemType Directory -Path $tokenDir -Force | Out-Null
            }
            Set-Content -Path $tokenFile -Value $CurrentToken -Encoding UTF8 -NoNewline
        }
        catch {
            Write-Verbose "Failed to persist provided Influx token: $($_.Exception.Message)"
        }
        return $CurrentToken
    }

    if (Test-Path $tokenFile) {
        try {
            $cachedToken = (Get-Content -Path $tokenFile -Raw -Encoding UTF8).Trim()
            if (-not [string]::IsNullOrWhiteSpace($cachedToken)) {
                if ($cachedToken -eq "EXISTS_BUT_UNKNOWN") {
                    Write-Warning "检测到历史无效 Influx token 占位值，已忽略缓存并尝试重新解析。"
                    Remove-Item -Path $tokenFile -Force -ErrorAction SilentlyContinue
                }
                else {
                Write-Host "[*] 使用已缓存的 InfluxDB token。" -ForegroundColor DarkGray
                return $cachedToken
                }
            }
        }
        catch {
            Write-Verbose "Failed to read cached Influx token: $($_.Exception.Message)"
        }
    }

    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $createResult = Try-CreateInfluxToken -InfluxExe $InfluxExe -CurrentToken ""
        if ($createResult.alreadyExists) {
            Write-Warning "InfluxDB 已存在管理员 token，但当前无法自动恢复。请通过 -InfluxToken 传入已有 token，或删除 Influx 数据目录后重建。"
            return ""
        }

        $created = [string]$createResult.token
        if (-not [string]::IsNullOrWhiteSpace($created)) {
            try {
                if (-not (Test-Path $tokenDir)) {
                    New-Item -ItemType Directory -Path $tokenDir -Force | Out-Null
                }
                Set-Content -Path $tokenFile -Value $created -Encoding UTF8 -NoNewline
            }
            catch {
                Write-Verbose "Failed to persist created Influx token: $($_.Exception.Message)"
            }
            return $created
        }

        if ($attempt -lt $MaxAttempts) {
            Write-Host "[*] InfluxDB token 获取失败，${RetrySeconds}s 后重试 (${attempt}/${MaxAttempts})..." -ForegroundColor DarkGray
            Start-Sleep -Seconds $RetrySeconds
        }
    }

    return ""
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
                if (-not [string]::IsNullOrWhiteSpace($WorkerInfluxToken)) {
                    $env:HMHT_INFLUXDB_ENABLED = "true"
                    $env:HMHT_INFLUXDB_TOKEN = $WorkerInfluxToken
                }
                else {
                    $env:HMHT_INFLUXDB_ENABLED = "false"
                    Remove-Item Env:HMHT_INFLUXDB_TOKEN -ErrorAction SilentlyContinue
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
                Stop-ProcessTreeGracefully -ProcessId $childProc.Id -GraceSeconds 8
                break
            }

            Write-WorkerStatus -StatusFile $statusFile -Name $WorkerName -Kind $WorkerKind -State "Running" -ShellPid $PID -ChildPid $childProc.Id -Message $message
            Wait-Process -Id $childProc.Id -Timeout 1 -ErrorAction SilentlyContinue
        }

        if (-not $childProc.HasExited) {
            Wait-Process -Id $childProc.Id -Timeout 2 -ErrorAction SilentlyContinue
        }

        $exitCode = if ($childProc.HasExited) { $childProc.ExitCode } else { 1 }
        if ($null -eq $exitCode) { $exitCode = 1 }
        $endState = if ($exitCode -eq 0) { "Stopped" } else { "Failed" }
        $endMessage = if ($exitCode -ne 0) { "$message (exit $exitCode)" } else { $message }

        Write-WorkerStatus -StatusFile $statusFile -Name $WorkerName -Kind $WorkerKind -State $endState -ShellPid $PID -ChildPid $childProc.Id -ExitCode $exitCode -Message $endMessage
        return $exitCode
    }
    catch {
        Write-WorkerStatus -StatusFile $statusFile -Name $WorkerName -Kind $WorkerKind -State "Failed" -ShellPid $PID -ChildPid 0 -ExitCode 1 -Message $_.Exception.Message
        throw
    }
    finally {
        if ($childProc -and -not $childProc.HasExited) {
            Stop-ProcessTreeGracefully -ProcessId $childProc.Id -GraceSeconds 3
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

# Auto-detect JavaDir from script location if not explicitly provided
if ([string]::IsNullOrWhiteSpace($JavaDir)) {
    $JavaDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
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

# 预防：若无缓存 token，清理 InfluxDB 数据目录以避免 "token name already exists" 冲突
if ((Test-Path $influxExe) -and [string]::IsNullOrWhiteSpace($InfluxToken)) {
    $influxDataDir = Join-Path $InfluxDir "data"
    $influxTokenCache = Join-Path $influxDataDir "hmht-admin.token"
    if (-not (Test-Path $influxTokenCache) -and (Test-Path $influxDataDir)) {
        Write-Warning "未找到 InfluxDB token 缓存但数据目录已存在，清理数据目录以避免 token 冲突..."
        Remove-Item -Path $influxDataDir -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "[*] InfluxDB 数据目录已清理，启动时将重建。" -ForegroundColor DarkGray
    }
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
        $InfluxToken = Resolve-InfluxToken -InfluxExe $influxExe -InfluxDir $InfluxDir -CurrentToken $InfluxToken
        if ([string]::IsNullOrWhiteSpace($InfluxToken)) {
            Write-Warning "未获取到 InfluxDB token，Java Worker 将禁用 Influx 集成（MQTT 转发不受影响）。可通过 -InfluxToken 显式传入以启用。"
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
            Stop-ProcessTreeGracefully -ProcessId ([int]$st.childPid) -GraceSeconds 2
        }
        if ($st.shellPid -and $st.shellPid -gt 0) {
            Stop-ProcessTreeGracefully -ProcessId ([int]$st.shellPid) -GraceSeconds 2
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
            Stop-ProcessTreeGracefully -ProcessId $WtOwnerProcess.Id -GraceSeconds 2
        }
    }

    try {
        $wtByCmd = Get-CimInstance Win32_Process -Filter "Name='WindowsTerminal.exe'" | Where-Object {
            $_.CommandLine -like "*$WindowName*"
        }
        foreach ($item in $wtByCmd) {
            Stop-ProcessTreeGracefully -ProcessId $item.ProcessId -GraceSeconds 2
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

        # Fail-fast: detect any failed worker and immediately trigger shutdown
        $failedServices = @()
        $allDone = $true
        foreach ($svc in $services) {
            $st = Read-ServiceStatus -Service $svc
            if ($st.state -eq "Failed") {
                $failedServices += $st
            }
            if ($st.state -in @("Starting", "Running", "Stopping", "Unknown")) {
                $allDone = $false
            }
        }

        if ($failedServices.Count -gt 0) {
            Write-Host ""
            Write-Host "==================== 服务启动失败 ====================" -ForegroundColor Red
            foreach ($f in $failedServices) {
                Write-Host "  [$($f.name)] ExitCode=$($f.exitCode)  $($f.message)" -ForegroundColor Red
            }
            Write-Host "======================================================" -ForegroundColor Red

            # Dump tail of Java log if available
            foreach ($f in $failedServices) {
                if ($f.kind -eq "java") {
                    $logFile = Join-Path $JavaDir "logs\hmht.log"
                    if (Test-Path $logFile) {
                        Write-Host ""
                        Write-Host "--- [$($f.name)] 最近日志 ($logFile) ---" -ForegroundColor Yellow
                        Get-Content -Path $logFile -Tail 40 -Encoding UTF8 | ForEach-Object { Write-Host $_ }
                        Write-Host "--- 日志结束 ---" -ForegroundColor Yellow
                    }
                }
            }
            break
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
