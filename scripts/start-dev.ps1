param(
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 5173,
    [string]$HostAddress = "127.0.0.1",
    [switch]$SkipInstall,
    [switch]$Restart,
    [int]$StartupTimeoutSec = 120
)

$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$FrontendDir = Join-Path $RootDir "frontend"
$TargetDir = Join-Path $RootDir "target"
$LogDir = Join-Path $RootDir "logs\dev"
$PidFile = Join-Path $TargetDir "dev-services.json"
$StopScript = Join-Path $PSScriptRoot "stop-dev.ps1"

function Resolve-RequiredCommand {
    param([string[]]$Names)

    foreach ($name in $Names) {
        $command = Get-Command $name -ErrorAction SilentlyContinue
        if ($command) {
            return $command.Source
        }
    }

    throw "Required command not found: $($Names -join ', ')"
}

function Get-PortListener {
    param([int]$Port)

    Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
}

function Wait-HttpReady {
    param(
        [string]$Url,
        [int]$TimeoutSec
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    do {
        try {
            Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3 | Out-Null
            return
        } catch {
            Start-Sleep -Seconds 1
        }
    } while ((Get-Date) -lt $deadline)

    throw "Timed out waiting for $Url"
}

function Start-CmdService {
    param(
        [string]$Name,
        [string]$CommandFile
    )

    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $env:ComSpec
    $psi.Arguments = "/d /c `"$CommandFile`""
    $psi.WorkingDirectory = $RootDir
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true

    $process = [System.Diagnostics.Process]::Start($psi)
    if (-not $process) {
        throw "Failed to start $Name"
    }

    return [ordered]@{
        name = $Name
        pid = $process.Id
        commandFile = $CommandFile
    }
}

if ($Restart -and (Test-Path $PidFile)) {
    & $StopScript
}

New-Item -ItemType Directory -Path $TargetDir, $LogDir -Force | Out-Null

$backendListener = Get-PortListener -Port $BackendPort
if ($backendListener) {
    throw "Backend port $BackendPort is already used by PID $($backendListener.OwningProcess). Run scripts\stop-dev.ps1 or change the port."
}

$frontendListener = Get-PortListener -Port $FrontendPort
if ($frontendListener) {
    throw "Frontend port $FrontendPort is already used by PID $($frontendListener.OwningProcess). Run scripts\stop-dev.ps1 or change the port."
}

$mvn = Resolve-RequiredCommand -Names @("mvn.cmd", "mvn")
$npm = Resolve-RequiredCommand -Names @("npm.cmd", "npm")

if (-not $SkipInstall -and -not (Test-Path (Join-Path $FrontendDir "node_modules"))) {
    Write-Output "Installing frontend dependencies..."
    Push-Location $FrontendDir
    try {
        & $npm install
    } finally {
        Pop-Location
    }
}

$backendLog = Join-Path $LogDir "backend.log"
$frontendLog = Join-Path $LogDir "frontend.log"
$backendCmd = Join-Path $TargetDir "dev-backend.cmd"
$frontendCmd = Join-Path $TargetDir "dev-frontend.cmd"
$mavenRepo = Join-Path $TargetDir ".m2"

@"
@echo off
cd /d "$RootDir"
"$mvn" "-Dmaven.repo.local=$mavenRepo" spring-boot:run >> "$backendLog" 2>&1
"@ | Set-Content -Path $backendCmd -Encoding ASCII

@"
@echo off
cd /d "$FrontendDir"
"$npm" run dev -- --host $HostAddress --port $FrontendPort >> "$frontendLog" 2>&1
"@ | Set-Content -Path $frontendCmd -Encoding ASCII

$services = @()
$services += Start-CmdService -Name "backend" -CommandFile $backendCmd
$services += Start-CmdService -Name "frontend" -CommandFile $frontendCmd

try {
    Wait-HttpReady -Url "http://$HostAddress`:$BackendPort/" -TimeoutSec $StartupTimeoutSec
    Wait-HttpReady -Url "http://$HostAddress`:$FrontendPort/login" -TimeoutSec $StartupTimeoutSec

    $apiProxyStatus = $null
    try {
        Invoke-WebRequest -Uri "http://$HostAddress`:$FrontendPort/api/customers" -UseBasicParsing -TimeoutSec 5 | Out-Null
        $apiProxyStatus = "unexpected-success"
    } catch {
        $apiProxyStatus = $_.Exception.Response.StatusCode.value__
    }

    $state = [ordered]@{
        startedAt = (Get-Date).ToString("s")
        root = $RootDir
        backendUrl = "http://$HostAddress`:$BackendPort"
        frontendUrl = "http://$HostAddress`:$FrontendPort"
        apiProxyCustomersWithoutToken = $apiProxyStatus
        logs = [ordered]@{
            backend = $backendLog
            frontend = $frontendLog
        }
        services = $services
    }

    $state | ConvertTo-Json -Depth 5 | Set-Content -Path $PidFile -Encoding UTF8

    Write-Output ""
    Write-Output "Development services started."
    Write-Output "Backend : http://$HostAddress`:$BackendPort"
    Write-Output "Frontend: http://$HostAddress`:$FrontendPort"
    Write-Output "API proxy check /api/customers without token: $apiProxyStatus"
    Write-Output "PID file: $PidFile"
    Write-Output "Logs: $LogDir"
} catch {
    Write-Output "Startup failed. Stopping started processes..."
    $services | ConvertTo-Json -Depth 5 | Set-Content -Path $PidFile -Encoding UTF8
    & $StopScript
    throw
}
