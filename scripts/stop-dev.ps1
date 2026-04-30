param(
    [switch]$KeepPidFile
)

$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$PidFile = Join-Path $RootDir "target\dev-services.json"

function Get-ProcessInfo {
    param([int]$ProcessId)

    Get-CimInstance Win32_Process -Filter "ProcessId=$ProcessId" -ErrorAction SilentlyContinue
}

function Test-ProjectProcess {
    param(
        [int]$ProcessId,
        [string[]]$Hints
    )

    $currentProcessId = $ProcessId
    $visited = New-Object System.Collections.Generic.HashSet[int]

    for ($i = 0; $i -lt 6 -and $currentProcessId -gt 0; $i++) {
        if (-not $visited.Add($currentProcessId)) {
            return $false
        }

        $info = Get-ProcessInfo -ProcessId $currentProcessId
        if (-not $info) {
            return $false
        }

        $commandLine = [string]$info.CommandLine
        foreach ($hint in $Hints) {
            if ($hint -and $commandLine.Contains($hint)) {
                return $true
            }
        }

        $currentProcessId = [int]$info.ParentProcessId
    }

    return $false
}

function Stop-DevProcess {
    param(
        [int]$ProcessId,
        [string[]]$Hints
    )

    $info = Get-ProcessInfo -ProcessId $ProcessId
    if (-not $info) {
        return
    }

    if (-not (Test-ProjectProcess -ProcessId $ProcessId -Hints $Hints)) {
        Write-Output "Skipped PID $ProcessId because it does not match this project."
        return
    }

    $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if ($process) {
        Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
        Write-Output "Stopped PID $ProcessId ($($process.ProcessName))"
    }
}

function Get-PortFromUrl {
    param([string]$Url)

    try {
        return ([System.Uri]$Url).Port
    } catch {
        return $null
    }
}

if (-not (Test-Path $PidFile)) {
    Write-Output "No dev service PID file found: $PidFile"
    return
}

$state = Get-Content -Path $PidFile -Raw | ConvertFrom-Json
$hints = @(
    $RootDir,
    (Join-Path $RootDir "frontend"),
    (Join-Path $RootDir "target\dev-backend.cmd"),
    (Join-Path $RootDir "target\dev-frontend.cmd")
)

$targetPids = New-Object System.Collections.Generic.HashSet[int]
foreach ($service in $state.services) {
    if ($service.pid) {
        [void]$targetPids.Add([int]$service.pid)
    }
}

$ports = @(
    (Get-PortFromUrl -Url $state.backendUrl),
    (Get-PortFromUrl -Url $state.frontendUrl)
) | Where-Object { $_ }

foreach ($port in $ports) {
    $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($listener in $listeners) {
        [void]$targetPids.Add([int]$listener.OwningProcess)
    }
}

foreach ($targetPid in $targetPids) {
    Stop-DevProcess -ProcessId $targetPid -Hints $hints
}

if (-not $KeepPidFile) {
    Remove-Item -Path $PidFile -Force -ErrorAction SilentlyContinue
}

Write-Output "Development services stopped."
