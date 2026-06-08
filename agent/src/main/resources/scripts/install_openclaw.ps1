param (
    [switch]$SkipAdminCheck = $false,
    [string]$NextGpuHomeOverride = ""
)

# ---------------------------------
# Self-elevating PowerShell launcher
# ---------------------------------
if (
    -not $SkipAdminCheck -and
    -not ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
        [Security.Principal.WindowsBuiltInRole]::Administrator
    )
) {
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "powershell.exe"
    $argList = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "`"$PSCommandPath`"")
    if ($NextGpuHomeOverride) { $argList += @("-NextGpuHomeOverride", "`"$NextGpuHomeOverride`"") }

    $psi.Arguments = $argList -join " "
    $psi.Verb = "runas"
    try {
        [System.Diagnostics.Process]::Start($psi) | Out-Null
    } catch {
        exit 1
    }
    exit 0
}

# Force clean text encoding WITHOUT the hidden Byte Order Mark
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[Console]::OutputEncoding = $utf8NoBom
$OutputEncoding = $utf8NoBom

# -------------------------
# Initialization & Paths
# -------------------------
if ([string]::IsNullOrWhiteSpace($NextGpuHomeOverride)) {
    $NextGpuHome = Join-Path $env:LOCALAPPDATA "NextGPU"
} else {
    $NextGpuHome = $NextGpuHomeOverride
}

if (!(Test-Path $NextGpuHome)) {
    New-Item -ItemType Directory -Force -Path $NextGpuHome | Out-Null
}

$StateFile = Join-Path $NextGpuHome "openclaw_state.json"
$LogFile = Join-Path $NextGpuHome "openclaw_debug.log"

# -------------------------
# STATE FUNCTIONS
# -------------------------
function Get-State {
    if (Test-Path $StateFile) {
        try {
            $content = Get-Content -Path $StateFile -Raw -ErrorAction Stop
            if (-not [string]::IsNullOrWhiteSpace($content)) {
                return $content | ConvertFrom-Json
            }
        } catch {
            Write-Host "Warning: State file corrupted or empty. Resetting."
        }
    }
    return [PSCustomObject]@{
        initialized = $false
        progressPercentage = 0
        currentStepName = "init"
        status = "PENDING"
        error = $null
        steps = @{}
    }
}

function Save-State($stateObj) {
    $stateObj | ConvertTo-Json -Depth 5 | Out-File -FilePath $StateFile -Encoding utf8 -Force
}

function Update-Step($StepKey, $StepDesc, $Status, $Progress, $ErrorMessage = $null) {
    $state = Get-State

    if ($Progress -gt $state.progressPercentage -or $Status -eq "COMPLETED") {
        $state.progressPercentage = $Progress
    }

    $state.currentStepName = $StepKey

    if ($state.steps -is [System.Management.Automation.PSCustomObject]) {
        $newSteps = @{}
        $state.steps.PSObject.Properties | ForEach-Object {
            $newSteps[$_.Name] = $_.Value
        }
        $state.steps = $newSteps
    } elseif (-not $state.steps) {
        $stepsHash = @{}
        $state | Add-Member -MemberType NoteProperty -Name "steps" -Value $stepsHash -Force
    }

    $state.steps[$StepKey] = $Status

    if ($Status -eq "FAILED") {
        $state.status = "FAILED"
        $state.error = $ErrorMessage
        Save-State $state
        Write-Host "CRITICAL ERROR [$StepKey]: $ErrorMessage" -ForegroundColor Red
        Stop-Transcript
        exit 1
    } elseif ($Status -eq "COMPLETED") {
        if ($Progress -ge 100) {
            $state.status = "COMPLETED"
            $state.initialized = $true
        } else {
            $state.status = "RUNNING"
        }
    } else {
        $state.status = "RUNNING"
    }

    Save-State $state
    Write-Host "[$Progress%] $StepDesc - $Status"
}

# ----------------------------------------------------
# RESET LOGIC & TRANSCRIPT
# ----------------------------------------------------
$resetState = [PSCustomObject]@{
    initialized = $false
    progressPercentage = 0
    currentStepName = "init"
    status = "RUNNING"
    error = $null
    steps = @{}
}
Save-State $resetState
if (Test-Path $LogFile) { Clear-Content $LogFile -Force }

Start-Transcript -Path $LogFile -Append -Force

# ----------------------------------------------------
# INSTALLATION HELPERS
# ----------------------------------------------------
function Is-StepDone($StepKey) {
    $state = Get-State
    if ($state.steps -is [System.Management.Automation.PSCustomObject]) {
        return ($state.steps.$StepKey -eq "COMPLETED")
    } elseif ($state.steps -is [System.Collections.Hashtable]) {
        return ($state.steps[$StepKey] -eq "COMPLETED")
    }
    return $false
}

function Fail-StepFromExitCode($StepKey, $StepDesc, $ExitCode) {
    $errMsg = "Command failed with Exit Code $ExitCode during step '$StepDesc'"
    $state = Get-State
    $currentProgress = $state.progressPercentage
    Update-Step $StepKey $StepDesc "FAILED" $currentProgress $errMsg
}

function RunWSL($cmd, $StepKey, $StepDesc) {
    Write-Host "Executing WSL command for: $StepDesc" -ForegroundColor Cyan
    # Explicitly targeting the nextgpu distro
    $cmd | wsl -d nextgpu -- bash -c "tr -d '\r' | bash -le" 2>&1 | ForEach-Object { Write-Host $_ }
    if ($LASTEXITCODE -ne 0) {
        Fail-StepFromExitCode $StepKey $StepDesc $LASTEXITCODE
    }
}

function Get-WSL($cmd) {
    $output = wsl -d nextgpu -- bash -lc "$cmd" 2>&1
    if ($null -eq $output) { return "" }
    return ($output | Out-String).Trim().Replace("`0", "")
}

# -------------------------
# MAIN FLOW
# -------------------------
Update-Step "init" "Initializing OpenClaw Setup" "RUNNING" 5

# -------------------------
# 1. OS Prerequisites
# -------------------------
if (-not (Is-StepDone "prereqs")) {
    Update-Step "prereqs" "Installing OS prerequisites" "RUNNING" 15

    # We use sudo here safely because nextgpu user has NOPASSWD setup
    RunWSL @'
set -e
sudo apt-get update && sudo apt-get upgrade -y
sudo apt-get install curl build-essential jq expect -y
'@ "prereqs" "Installing curl, build-essential, jq, and expect"

    Update-Step "prereqs" "Installing OS prerequisites" "COMPLETED" 25
}

# -------------------------
# 2. NVM & Node.js Setup
# -------------------------
if (-not (Is-StepDone "nvm_setup")) {
    Update-Step "nvm_setup" "Installing NVM and Node.js" "RUNNING" 35

    RunWSL @'
set -e

# FIX: Remove conflicting prefix and globalconfig settings from previous Node installations
if [ -f "$HOME/.npmrc" ]; then
    echo "Cleaning up conflicting .npmrc configurations..."
    sed -i '/^prefix/d' "$HOME/.npmrc" || true
    sed -i '/^globalconfig/d' "$HOME/.npmrc" || true
fi

# FIX: Unset any lingering environment variables that might confuse NVM
unset NPM_CONFIG_PREFIX

# Install NVM
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/master/install.sh | bash
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"

# Install and configure LTS Node.js
nvm install --lts
nvm use --delete-prefix --lts # --delete-prefix acts as a final safety net
nvm alias default 'lts/*'
node --version
npm --version
'@ "nvm_setup" "Installing NVM and LTS Node.js"

    Update-Step "nvm_setup" "Installing NVM and Node.js" "COMPLETED" 50
}

# -------------------------
# 3. OpenClaw Core Installation
# -------------------------
if (-not (Is-StepDone "openclaw_install")) {
    Update-Step "openclaw_install" "Installing OpenClaw CLI" "RUNNING" 60

    RunWSL @'
set -e
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"

# Download the installation script locally
curl -o install_openclaw.sh -fsSL https://openclaw.ai/install.sh

# A balanced 20-second timeout acts as a fallback safety net if an ANSI redraw hides a string
expect -c "
set timeout 20
spawn bash install_openclaw.sh

expect {
    \"*gateway token now?*\"     { send \"\r\"; exp_continue }
    \"*Create Session store*\"    { send \"\r\"; exp_continue }
    \"*unavailable skills*\"      { send \"n\r\"; exp_continue }
    \"*shell completion*\"        { send \"n\r\"; exp_continue }
    \"*gateway service now?*\"    { send \"\r\"; exp_continue }
    \"*service runtime*\"         { send \"\r\"; exp_continue }
    timeout                      { send \"\r\"; exp_continue }
    eof
}
"

openclaw --version
'@ "openclaw_install" "Downloading and installing OpenClaw"

    Update-Step "openclaw_install" "Installing OpenClaw CLI" "COMPLETED" 70
}
# -------------------------
# 4. Model Scan & Configuration
# -------------------------
if (-not (Is-StepDone "openclaw_config")) {
    Update-Step "openclaw_config" "Configuring OpenClaw Workspace" "RUNNING" 75

    Write-Host "Scanning local Ollama instance for models..." -ForegroundColor Cyan
    $OllamaOutput = Get-WSL "ollama list"
    $PrimaryModel = "qwen2.5:3b" # Safe fallback

    $FallbackList = @()
    $ModelsDictEntries = @()
    $ProviderModelsList = @()

    $OllamaLines = $OllamaOutput -split "`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    if ($OllamaLines.Count -gt 1) {
        $FirstModelLine = $OllamaLines[1] -split '\s+'
        if (-not [string]::IsNullOrWhiteSpace($FirstModelLine[0])) {
            $PrimaryModel = $FirstModelLine[0].Trim()
            Write-Host "Discovered primary local model: $PrimaryModel" -ForegroundColor Green
        }
        $ProviderModelsList += "{ `"id`": `"$PrimaryModel`", `"name`": `"$PrimaryModel`" }"

        if ($OllamaLines.Count -gt 2) {
            for ($i = 2; $i -lt $OllamaLines.Count; $i++) {
                $LineItems = $OllamaLines[$i] -split '\s+'
                if (-not [string]::IsNullOrWhiteSpace($LineItems[0])) {
                    $ModelName = $LineItems[0].Trim()
                    $FallbackList += "`"ollama/$ModelName`""
                    $ModelsDictEntries += "`"ollama/$ModelName`": {}"
                    $ProviderModelsList += "{ `"id`": `"$ModelName`", `"name`": `"$ModelName`" }"
                }
            }
        }
    } else {
        $ProviderModelsList += "{ `"id`": `"$PrimaryModel`", `"name`": `"$PrimaryModel`" }"
    }

    $FallbackArrayJson = $FallbackList -join ", "
    $ModelsDictEntries = @("`"ollama/$PrimaryModel`": {}") + $ModelsDictEntries
    $FallbackModelsJson = $ModelsDictEntries -join ",`n        "
    $ProviderModelsJson = $ProviderModelsList -join ",`n          "

    $WslUser = Get-WSL "whoami"
    $IsoTimestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fff'Z'")

    # NEW: Generate our own highly secure 64-character hex token!
    $GatewayToken = [guid]::NewGuid().ToString("N") + [guid]::NewGuid().ToString("N")

    # Construct the complete pristine openclaw.json template WITH our injected token
    $OpenclawJson = @"
{
  "agents": {
    "defaults": {
      "workspace": "/home/$WslUser/.openclaw/workspace",
      "model": {
        "primary": "ollama/$PrimaryModel",
        "fallbacks": [$FallbackArrayJson]
      },
      "models": {
        $FallbackModelsJson
      }
    }
  },
  "gateway": {
    "mode": "local",
    "auth": {
      "token": "$GatewayToken"
    }
  },
 "models": {
     "providers": {
       "ollama": {
         "api": "ollama",
         "baseUrl": "http://127.0.0.1:11434",
         "models": [
           $ProviderModelsJson
         ]
       }
     }
   },
  "auth": {
    "profiles": {
      "ollama:default": {
        "provider": "ollama",
        "mode": "api_key"
      }
    }
  },
  "wizard": {
    "lastRunAt": "$IsoTimestamp",
    "lastRunVersion": "2026.5.28",
    "lastRunCommand": "onboard",
    "lastRunMode": "local"
  },
  "meta": {
    "lastTouchedVersion": "2026.5.28",
    "lastTouchedAt": "$IsoTimestamp"
  }
}
"@

    $JsonBytes = [System.Text.Encoding]::UTF8.GetBytes($OpenclawJson)
    $Base64Json = [Convert]::ToBase64String($JsonBytes)

    # Notice how much cleaner this WSL block is now without expect!
    RunWSL @"
set -e
echo '$Base64Json' | base64 -d > /home/$WslUser/.openclaw/openclaw.json
chmod 600 /home/$WslUser/.openclaw/openclaw.json

export NVM_DIR="/home/$WslUser/.nvm"
[ -s "`$NVM_DIR/nvm.sh" ] && \. "`$NVM_DIR/nvm.sh"

export XDG_RUNTIME_DIR=/run/user/`$(id -u)
export DBUS_SESSION_BUS_ADDRESS=unix:path=`${XDG_RUNTIME_DIR}/bus

echo "Running doctor --fix..."
yes "" | CI=true openclaw doctor --fix >/dev/null 2>&1 || true

echo "Restarting OpenClaw Gateway to apply our custom token..."
openclaw gateway restart || openclaw gateway start

"@ "openclaw_config" "Writing configuration profiles and launching gateway"

    Update-Step "openclaw_config" "Configuring OpenClaw Workspace" "COMPLETED" 90
}

# ----------------------------------
# 10. All systems ready
# ----------------------------------
Update-Step "finished" "OpenClaw configuration complete" "COMPLETED" 100

Stop-Transcript