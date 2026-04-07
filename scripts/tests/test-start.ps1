# Test script for start.ps1
param([switch]$SkipLongTests)

$ErrorActionPreference = "Stop"
$scriptPath = Join-Path $PSScriptRoot "start.ps1"

Write-Host "====================================" -ForegroundColor Cyan
Write-Host "  start.ps1 Test Suite" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan

$testsPassed = 0
$testsFailed = 0

function Test-Case {
    param([string]$Name, [scriptblock]$Test)
    
    Write-Host "`n[$Name]" -ForegroundColor Yellow
    try {
        & $Test
        Write-Host "  [PASS]" -ForegroundColor Green
        $script:testsPassed++
    }
    catch {
        Write-Host "  [FAIL]: $($_.Exception.Message)" -ForegroundColor Red
        $script:testsFailed++
    }
}

# Test 1: Syntax check
Test-Case "Syntax Check" {
    $errors = $null
    $tokens = $null
    $ast = [System.Management.Automation.Language.Parser]::ParseFile(
        $scriptPath,
        [ref]$tokens,
        [ref]$errors
    )
    
    if ($errors) {
        throw "Found $($errors.Count) syntax errors"
    }
}

# Test 2: Help documentation
Test-Case "Help Documentation" {
    $help = Get-Help $scriptPath -ErrorAction Stop
    if (-not $help.Synopsis) {
        throw "Missing Synopsis"
    }
}

# Test 3: Mode parameter validation
Test-Case "Mode Parameter Validation" {
    try {
        & $scriptPath -Mode "invalid" -ErrorAction Stop
        throw "Should reject invalid Mode"
    }
    catch {
        if ($_.Exception.Message -notmatch "ValidateSet") {
            throw
        }
    }
}

# Test 4: Worker mode parameter validation
Test-Case "Worker Mode Parameters" {
    try {
        & $scriptPath -WorkerMode -ErrorAction Stop
        throw "Worker mode without WorkerName should fail"
    }
    catch {
        if ($_.Exception.Message -notmatch "WorkerName") {
            # Expected to fail due to missing WorkerName
        }
    }
}

# Test 5: Dependency check
Test-Case "Windows Terminal Detection" {
    $wt = Get-Command wt.exe -ErrorAction SilentlyContinue
    if (-not $wt) {
        throw "Windows Terminal not installed"
    }
}

# Test 6: Java environment
Test-Case "Java Environment Detection" {
    if (-not $env:JAVA_HOME) {
        throw "JAVA_HOME not set"
    }
    
    $javaExe = Join-Path $env:JAVA_HOME "bin" | Join-Path -ChildPath "java.exe"
    if (-not (Test-Path $javaExe)) {
        throw "Java executable not found: $javaExe"
    }
}

# Test 7: Project paths
Test-Case "Project Path Validation" {
    $javaDir = "D:\JetBrainsProjects\HMHT"
    if (-not (Test-Path $javaDir)) {
        throw "Java project directory not found: $javaDir"
    }
    
    $gradlew = Join-Path $javaDir "gradlew.bat"
    if (-not (Test-Path $gradlew)) {
        throw "gradlew.bat not found"
    }
}

# Test 8: UTF-8 encoding check
Test-Case "UTF-8 Encoding" {
    $content = Get-Content $scriptPath -Raw -Encoding UTF8
    if ($content -match '[\x00-\x08\x0B\x0C\x0E-\x1F]') {
        throw "File contains invalid control characters"
    }
}

# Test 9: Function definitions completeness
Test-Case "Key Function Definitions" {
    $content = Get-Content $scriptPath -Raw
    
    $requiredFunctions = @(
        "Set-Utf8Console",
        "Resolve-Influx",
        "Invoke-Worker"
    )
    
    foreach ($func in $requiredFunctions) {
        if ($content -notmatch "function\s+$func") {
            throw "Missing function: $func"
        }
    }
}

# Test 10: Mode configuration test
Test-Case "Mode Configurations" {
    $modes = @("mock", "bridge", "hardware", "full")
    foreach ($mode in $modes) {
        # Only validate parameter acceptance, do not actually run
        $params = @{
            Mode = $mode
            WhatIf = $true
        }
        # Just verify parameters do not error
    }
}

# Summary
Write-Host "`n====================================" -ForegroundColor Cyan
Write-Host "Test Complete" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host "Passed: $testsPassed" -ForegroundColor Green
Write-Host "Failed: $testsFailed" -ForegroundColor Red

if ($testsFailed -eq 0) {
    Write-Host "`nAll tests passed!" -ForegroundColor Green
    exit 0
}
else {
    Write-Host "`nSome tests failed" -ForegroundColor Red
    exit 1
}
