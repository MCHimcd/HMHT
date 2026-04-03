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
$startScript = Join-Path $repoRoot 'start.ps1'
$legacyScript = Join-Path $repoRoot 'start-all.ps1'

Assert-True (Test-Path $startScript) 'start.ps1 must exist.'
Assert-True (-not (Test-Path $legacyScript)) 'start-all.ps1 should be renamed to start.ps1.'

$content = Get-Content -Path $startScript -Raw -Encoding UTF8

Assert-True ($content -match '\[ValidateSet\("mock",\s*"bridge"\)\]') 'Mode must keep mock/bridge validate set.'
Assert-True ($content -match '\[switch\]\$WorkerMode') 'start.ps1 must support WorkerMode for tab workers.'
Assert-True ($content -match 'Get-Command\s+"wt"') 'start.ps1 must require Windows Terminal.'
Assert-True ($content -match 'UTF8') 'start.ps1 must set UTF-8 related behavior.'
Assert-True ($content -match 'Stop-Requested|StopRequested|stop') 'start.ps1 should include stop orchestration logic.'
Assert-True ($content -match 'Render|Status') 'start.ps1 should expose status rendering logic for main terminal.'

Write-Host '[PASS] start.ps1 script-level checks passed.' -ForegroundColor Green
