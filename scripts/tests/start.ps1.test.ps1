$ErrorActionPreference = 'Stop'

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw "[ASSERT FAILED] $Message"
    }
}

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$startScript = Join-Path $repoRoot 'scripts\start.ps1'
$legacyScript = Join-Path $repoRoot 'scripts\start-all.ps1'

Assert-True (Test-Path $startScript) 'start.ps1 must exist.'
Assert-True (-not (Test-Path $legacyScript)) 'start-all.ps1 should be renamed to start.ps1.'

$content = Get-Content -Path $startScript -Raw -Encoding UTF8

Assert-True ($content -match '\[ValidateSet\("mock",\s*"bridge",\s*"hardware",\s*"full"\)\]') 'Mode must keep mock/bridge/hardware/full validate set.'
Assert-True ($content -match '\[switch\]\$WorkerMode') 'start.ps1 must support WorkerMode for tab workers.'
Assert-True ($content -match 'Get-Command\s+"wt"') 'start.ps1 must require Windows Terminal.'
Assert-True ($content -match 'UTF8') 'start.ps1 must set UTF-8 related behavior.'
Assert-True ($content -match 'Stop-Requested|StopRequested|stop') 'start.ps1 should include stop orchestration logic.'
Assert-True ($content -match 'Render|Status') 'start.ps1 should expose status rendering logic for main terminal.'
Assert-True (
    $content -match 'if \(-not \[string\]::IsNullOrWhiteSpace\(\$InfluxToken\)\) \{\s*\$tabArgs \+= @\("-WorkerInfluxToken", \$InfluxToken\)\s*\}'
) 'start.ps1 must append -WorkerInfluxToken only when token is non-empty.'
Assert-True (-not ($content -match '\$pid\s*=')) 'start.ps1 should avoid assigning to $pid because it conflicts with read-only automatic variable $PID.'
Assert-True ($content -match 'function\s+ConvertTo-CommandLineArgumentString') 'start.ps1 must include argument quoting helper for wt command line.'
Assert-True ($content -match 'Start-Process\s+-FilePath\s+"wt"\s+-ArgumentList\s+\$tabArgumentLine') 'start.ps1 must pass wt arguments as pre-quoted argument line.'
Assert-True (
    $content -match 'if \(-not \[string\]::IsNullOrWhiteSpace\(\$WorkerInfluxToken\)\) \{\s*\$env:HMHT_INFLUXDB_ENABLED = "true"\s*\$env:HMHT_INFLUXDB_TOKEN = \$WorkerInfluxToken\s*\}\s*else \{\s*\$env:HMHT_INFLUXDB_ENABLED = "false"'
) 'start.ps1 should disable InfluxDB integration for Java worker when no valid token is available.'
Assert-True ($content -match 'function\s+Resolve-InfluxToken') 'start.ps1 must provide a token resolving helper for Influx startup.'
Assert-True ($content -match 'hmht-admin\.token') 'start.ps1 should persist/reuse Influx token cache file to avoid re-auth failures across restarts.'
Assert-True ($content -match 'Resolve-InfluxToken\s+-InfluxExe\s+\$influxExe\s+-InfluxDir\s+\$InfluxDir\s+-CurrentToken\s+\$InfluxToken') 'start.ps1 should resolve Influx token before launching following services.'
Assert-True (-not ($content -match 'return\s+"EXISTS_BUT_UNKNOWN"')) 'start.ps1 must not return EXISTS_BUT_UNKNOWN as a usable Influx token.'
Assert-True ($content -match '\$createResult\.alreadyExists') 'start.ps1 should explicitly detect token-exists conflict and stop auto-injection.'
Assert-True ($content -match 'influxTokenCache.*hmht-admin\.token') 'start.ps1 should pre-check token cache and clean InfluxDB data dir to prevent token conflict.'
Assert-True ($content -match '\$rawText\s*=.*Out-String') 'Try-CreateInfluxToken should convert all output to string before pattern matching to detect errors from both stdout and stderr.'
Assert-True ($content -match 'failedServices') 'start.ps1 must detect failed workers and trigger immediate shutdown (fail-fast).'
Assert-True ($content -match 'logs\\hmht\.log') 'start.ps1 should dump tail of Java log file on failure.'

Write-Host '[PASS] start.ps1 script-level checks passed.' -ForegroundColor Green
