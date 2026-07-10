param (
    [switch]$OverwriteExistingWsl = $false,
    [switch]$SkipAdminCheck = $false,
    [string]$NextGpuHomeOverride = "",
    [switch]$SkipGpuCheck = $false,
    [string]$InstallProfile = "ai_hub"
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
    # Re-pass arguments, ensuring switches are handled correctly
    $argList = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "`"$PSCommandPath`"")
    if ($OverwriteExistingWsl) { $argList += "-OverwriteExistingWsl" }
    if ($NextGpuHomeOverride) { $argList += @("-NextGpuHomeOverride", "`"$NextGpuHomeOverride`"") }
    if ($SkipGpuCheck) { $argList += "-SkipGpuCheck" }
    if ($InstallProfile) { $argList += @("-InstallProfile", "`"$InstallProfile`"") }

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
$OllamaPort = 11434
$ComfyPort = 8188
$SttToolPort = 8177

if ([string]::IsNullOrWhiteSpace($NextGpuHomeOverride)) {
    $NextGpuHome = Join-Path $env:LOCALAPPDATA "NextGPU"
} else {
    $NextGpuHome = $NextGpuHomeOverride
}

if (!(Test-Path $NextGpuHome)) {
    New-Item -ItemType Directory -Force -Path $NextGpuHome | Out-Null
}

$StateFile = Join-Path $NextGpuHome "state.json"
$CredentialFile = Join-Path $NextGpuHome "wsl_credentials.txt"
$LogFile = Join-Path $NextGpuHome "install_debug.log"

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
if ($OverwriteExistingWsl) {
    Write-Host "Start Fresh requested. Resetting state and wiping old logs..." -ForegroundColor Yellow
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
}

Start-Transcript -Path $LogFile -Append -Force

# ----------------------------------------------------
# PREFLIGHT FUNCTIONS
# ----------------------------------------------------
$PreflightResults = @{
    passed = @()
    warnings = @()
    failures = @()
}

function Add-Pass($check, $detail) {
    $PreflightResults.passed += @{ check = $check; detail = $detail }
    Write-Host "[PASSED] $check" -ForegroundColor Green
    if ($detail) { Write-Host "    $detail" -ForegroundColor Gray }
}

function Add-Warning($check, $detail, $remediation) {
    $PreflightResults.warnings += @{ check = $check; detail = $detail; remediation = $remediation }
    Write-Host "[WARNING] $check" -ForegroundColor Yellow
    if ($detail) { Write-Host "    $detail" -ForegroundColor Yellow }
    if ($remediation) { Write-Host "    Fix: $remediation" -ForegroundColor DarkYellow }
}

function Add-Failure($check, $detail, $remediation) {
    $PreflightResults.failures += @{ check = $check; detail = $detail; remediation = $remediation }
    Write-Host "[FAILURE] $check" -ForegroundColor Red
    if ($detail) { Write-Host "    $detail" -ForegroundColor Red }
    if ($remediation) { Write-Host "    Fix: $remediation" -ForegroundColor DarkRed }
}

# ----------------------------------------------------
# PREFLIGHT CHECKS
# ----------------------------------------------------
Update-Step "preflight" "Running Preflight System Checks" "RUNNING" 1

Write-Host ("=" * 70)
Write-Host "  NextGPU Preflight Environment Check"
Write-Host ("=" * 70)

# 1. Windows Version (Windows 11 required)
$os = Get-CimInstance Win32_OperatingSystem
$buildNumber = [int]$os.BuildNumber
if ($buildNumber -ge 22000) {
    Add-Pass "Windows 11" "Build $buildNumber"
} else {
    Add-Failure "Windows version unsupported" "Build $buildNumber (Windows 11 requires build 22000+)" "Upgrade to Windows 11"
}

# 2. WSL
try {
    $wslVersion = wsl --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Add-Pass "WSL installed" "Functional"
    } else {
        Add-Warning "WSL version check failed" "WSL may not be fully installed" "Run: wsl --install"
    }
} catch {
    Add-Failure "WSL not installed" $_ "Run: wsl --install"
}

# 3. Disk Space
$drive = (Get-Item $NextGpuHome -ErrorAction SilentlyContinue).PSDrive
if (-not $drive) { $drive = Get-PSDrive -Name (Split-Path $NextGpuHome -Qualifier).TrimEnd(':') }
$freeSpaceGB = [math]::Round($drive.Free / 1GB, 2)
if ($freeSpaceGB -ge 50) {
    Add-Pass "Disk space" "$freeSpaceGB GB free"
} else {
    Add-Failure "Insufficient disk space" "$freeSpaceGB GB free (minimum: 50 GB)" "Free up space"
}

# 4. GPU
if (-not $SkipGpuCheck) {
    $gpus = Get-CimInstance Win32_VideoController | Where-Object { $_.Name -notmatch "Microsoft Basic|Remote Desktop|Hyper-V" }
    $nvidiaGpu = $gpus | Where-Object { $_.Name -match "NVIDIA" }
    if (-not $nvidiaGpu) {
        Add-Failure "No NVIDIA GPU detected" "CUDA requires NVIDIA" "Install NVIDIA GPU"
    } else {
        Add-Pass "NVIDIA GPU detected" $nvidiaGpu[0].Name
    }
}

# 5. Memory
$totalMemoryGB = [math]::Round($os.TotalVisibleMemorySize / 1MB, 1)
if ($totalMemoryGB -ge 11.9) {
    Add-Pass "System memory" "$totalMemoryGB GB total"
} else {
    Add-Failure "Insufficient memory" "$totalMemoryGB GB total (minimum: 12 GB)" "Upgrade RAM"
}

if ($PreflightResults.failures.Count -gt 0) {
    $failMsg = ($PreflightResults.failures | ForEach-Object { "$($_.check): $($_.detail)" }) -join "; "
    Update-Step "preflight" "Preflight Checks Failed" "FAILED" 1 $failMsg
}
Update-Step "preflight" "Preflight Checks Passed" "COMPLETED" 4

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

function Run-HostCommand($cmd, $StepKey, $StepDesc) {
    Write-Host "Executing host command for: $StepDesc" -ForegroundColor Cyan
    Invoke-Expression $cmd 2>&1 | ForEach-Object { Write-Host $_ }
    if ($LASTEXITCODE -ne 0) {
        Fail-StepFromExitCode $StepKey $StepDesc $LASTEXITCODE
    }
}

function RunWSL($cmd, $StepKey, $StepDesc) {
    Write-Host "Executing WSL command for: $StepDesc" -ForegroundColor Cyan
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
Update-Step "init" "Initializing Native WSL Setup" "RUNNING" 5
if (-not (Test-Path $CredentialFile)) {
    Update-Step "init" "Initializing Native WSL Setup" "FAILED" 5 "Missing credentials file."
}

$Password = (Get-Content -Path $CredentialFile -Raw).Trim()

# -------------------------
# 1. Install Ubuntu 24.04
# -------------------------
if (-not (Is-StepDone "wsl_install")) {
    Update-Step "wsl_install" "Configuring Windows Subsystem for Linux (WSL)" "RUNNING" 5
    
    Write-Host "Executing host command for: Updating WSL" -ForegroundColor Cyan
    wsl --update 2>&1 | ForEach-Object { Write-Host $_ }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Warning: wsl --update failed with exit code $LASTEXITCODE. Attempting to continue anyway..." -ForegroundColor Yellow
    }

    $existing = $false
    $existingList = wsl --list --quiet 2>$null | ForEach-Object { $_.Trim() }
    $existing = $existingList -contains "nextgpu"

    if ($existing -and $OverwriteExistingWsl) {
        Run-HostCommand "wsl --unregister nextgpu" "wsl_install" "Removing existing nextgpu distro"
    }

    if (-not $existing -or $OverwriteExistingWsl) {
        Update-Step "wsl_install" "Installing Ubuntu 24.04 Core" "RUNNING" 10
        Run-HostCommand "wsl --install -d Ubuntu-24.04 --no-launch" "wsl_install" "Installing Ubuntu-24.04"

        # Verify that the distribution was actually installed
        Write-Host "Verifying Ubuntu-24.04 registration..." -ForegroundColor Cyan
        $registrationRetry = 0
        $isRegistered = $false
        while ($registrationRetry -lt 5 -and -not $isRegistered) {
            $list = wsl --list --quiet 2>$null | ForEach-Object { $_.Trim() }
            if ($list -contains "Ubuntu-24.04") {
                $isRegistered = $true
            } else {
                $registrationRetry++
                Write-Host "Waiting for Ubuntu-24.04 to register (Attempt $registrationRetry/5)..." -ForegroundColor Yellow
                Start-Sleep -Seconds 5
            }
        }

        if (-not $isRegistered) {
            Update-Step "wsl_install" "Installing Ubuntu 24.04 Core" "FAILED" 10 "Ubuntu-24.04 distribution was not found after installation. Try running 'wsl --install -d Ubuntu-24.04' manually to see if it reports any errors."
        }

        Write-Host "Exporting and re-importing WSL instance for portability..."
        $ExportTar = Join-Path $NextGpuHome "nextgpu.tar"
        Run-HostCommand "wsl --export Ubuntu-24.04 `"$ExportTar`"" "wsl_install" "Exporting Ubuntu image"
        Run-HostCommand "wsl --unregister Ubuntu-24.04" "wsl_install" "Unregistering temporary Ubuntu distro"
        Run-HostCommand "wsl --import nextgpu `"$env:LOCALAPPDATA\WSL\nextgpu`" `"$ExportTar`"" "wsl_install" "Importing nextgpu distro"
        Remove-Item "$ExportTar" -Force
    }

    RunWSL @"
set -e
id -u nextgpu &>/dev/null || sudo useradd -m -s /bin/bash nextgpu
echo 'nextgpu:$Password' | sudo chpasswd
sudo usermod -aG sudo nextgpu
echo 'nextgpu ALL=(ALL) NOPASSWD:ALL' | sudo tee /etc/sudoers.d/nextgpu > /dev/null
"@ "wsl_install" "Configuring secure WSL user environment"

    RunWSL @"
cat << 'EOF' | sudo tee /etc/wsl.conf > /dev/null
[boot]
systemd=true

[user]
default=nextgpu
EOF
"@ "wsl_install" "Configuring WSL Boot Params"

    Write-Host "Restarting WSL instance to apply systemd and default-user configuration..." -ForegroundColor Yellow
    wsl --terminate nextgpu
    Start-Sleep -Seconds 3

    Update-Step "wsl_install" "Configuring Windows Subsystem for Linux (WSL)" "COMPLETED" 20
} else {
    Write-Host ">>> Step 'wsl_install' already COMPLETED. Resuming..." -ForegroundColor Gray
}

# -------------------------
# 2. NVIDIA Windows Driver Check
# -------------------------
if (-not (Is-StepDone "nvidia_windows_driver")) {
    Update-Step "nvidia_windows_driver" "Verifying NVIDIA Windows driver" "RUNNING" 22

    $nvidiaSmi = Get-Command nvidia-smi -ErrorAction SilentlyContinue

    if ($null -eq $nvidiaSmi) {
        Write-Host "nvidia-smi was not found on Windows. Attempting NVIDIA driver installation via winget..." -ForegroundColor Yellow

        $winget = Get-Command winget -ErrorAction SilentlyContinue
        if ($null -eq $winget) {
            Update-Step "nvidia_windows_driver" "Verifying NVIDIA Windows driver" "FAILED" 22 `
                    "NVIDIA driver is missing and winget is unavailable. Install the NVIDIA Windows driver manually, then rerun this installer."
        }

        winget install --id NVIDIA.Display.Driver --exact --silent --accept-package-agreements --accept-source-agreements 2>&1 | ForEach-Object {
            Write-Host $_
        }

        $nvidiaSmi = Get-Command nvidia-smi -ErrorAction SilentlyContinue
        if ($null -eq $nvidiaSmi) {
            Update-Step "nvidia_windows_driver" "Verifying NVIDIA Windows driver" "FAILED" 22 `
                    "NVIDIA driver installation did not expose nvidia-smi. Reboot Windows, confirm nvidia-smi works in PowerShell, then rerun this installer."
        }
    }

    nvidia-smi 2>&1 | ForEach-Object {
        Write-Host $_
    }

    if ($LASTEXITCODE -ne 0) {
        Update-Step "nvidia_windows_driver" "Verifying NVIDIA Windows driver" "FAILED" 22 `
                "nvidia-smi exists on Windows but failed to run. Update/reinstall the NVIDIA Windows driver, reboot, then rerun this installer."
    }

    Write-Host "Restarting WSL so it can bind to the NVIDIA Windows driver..." -ForegroundColor Yellow
    wsl --shutdown
    Start-Sleep -Seconds 8

    Update-Step "nvidia_windows_driver" "Verifying NVIDIA Windows driver" "COMPLETED" 25
} else {
    Write-Host ">>> Step 'nvidia_windows_driver' already COMPLETED. Resuming..." -ForegroundColor Gray
}

# -------------------------
# 3. CUDA (Ubuntu 24.04)
# -------------------------
if (-not (Is-StepDone "cuda_setup")) {
    Update-Step "cuda_setup" "Installing NVIDIA CUDA Toolkit" "RUNNING" 30

    RunWSL @'
set -e

echo "Checking whether the NVIDIA Windows driver is visible inside WSL..."
if [ ! -x /usr/lib/wsl/lib/nvidia-smi ]; then
  echo "ERROR: /usr/lib/wsl/lib/nvidia-smi was not found or is not executable."
  echo "The NVIDIA Windows driver is not visible inside WSL."
  echo "Install/update the NVIDIA Windows driver, run 'wsl --shutdown', then retry."
  exit 1
fi

sudo ln -sf /usr/lib/wsl/lib/nvidia-smi /usr/local/bin/nvidia-smi
nvidia-smi

sudo apt update
sudo DEBIAN_FRONTEND=noninteractive apt install -y build-essential gnupg ca-certificates wget sysbench

rm -f cuda-keyring_1.1-1_all.deb
wget --tries=3 --show-progress https://developer.download.nvidia.com/compute/cuda/repos/wsl-ubuntu/x86_64/cuda-keyring_1.1-1_all.deb

sudo dpkg -i cuda-keyring_1.1-1_all.deb
sudo apt-get update
sudo DEBIAN_FRONTEND=noninteractive apt-get -y install cuda-toolkit-12-6 cuda-compiler-12-6 cuda-nvcc-12-6

if [ ! -x /usr/local/cuda-12.6/bin/nvcc ]; then
  echo "ERROR: CUDA compiler installation completed, but /usr/local/cuda-12.6/bin/nvcc was not found."
  echo "Installed CUDA packages:"
  dpkg -l | grep -E 'cuda-toolkit|cuda-compiler|cuda-nvcc|cuda-cudart' || true
  echo "Searching for nvcc:"
  sudo find /usr -name nvcc -type f 2>/dev/null || true
  sudo find /usr/local -name nvcc -type f 2>/dev/null || true
  exit 1
fi

sudo ln -sfn /usr/local/cuda-12.6 /usr/local/cuda

cat <<'EOF' | sudo tee /etc/profile.d/cuda.sh > /dev/null
export CUDA_HOME=/usr/local/cuda
export CUDA_PATH=/usr/local/cuda
export PATH=/usr/local/cuda/bin:$PATH
export LD_LIBRARY_PATH=/usr/local/cuda/lib64:${LD_LIBRARY_PATH:-}
EOF

sudo chmod 644 /etc/profile.d/cuda.sh

export CUDA_HOME=/usr/local/cuda
export CUDA_PATH=/usr/local/cuda
export PATH=/usr/local/cuda/bin:$PATH
export LD_LIBRARY_PATH=/usr/local/cuda/lib64:${LD_LIBRARY_PATH:-}

command -v nvcc
nvcc --version
nvidia-smi
'@ "cuda_setup" "Installing NVIDIA CUDA Toolkit"

        Update-Step "cuda_setup" "Installing NVIDIA CUDA Toolkit" "COMPLETED" 45
    } else {
        Write-Host ">>> Step 'cuda_setup' already COMPLETED. Resuming..." -ForegroundColor Gray
    }

# -------------------------
# 4. Base packages for native AI stack
# -------------------------
if (-not (Is-StepDone "base_packages")) {
    Update-Step "base_packages" "Installing base packages for native AI services" "RUNNING" 48

    RunWSL @'
set -e
sudo apt update
sudo DEBIAN_FRONTEND=noninteractive apt install -y curl git python3 python3-pip python3-venv zstd
'@ "base_packages" "Installing base packages for native AI services"

    Update-Step "base_packages" "Installing base packages for native AI services" "COMPLETED" 52
} else {
    Write-Host ">>> Step 'base_packages' already COMPLETED. Resuming..." -ForegroundColor Gray
}

if ($InstallProfile -ne "provider") {
# -------------------------
# 5. Ollama and DeepSeek
# -------------------------
if (-not (Is-StepDone "ollama")) {
    Update-Step "ollama" "Installing Ollama and model assets natively in WSL" "RUNNING" 55

    RunWSL @'
set -e
if ! command -v ollama >/dev/null 2>&1; then
  echo "Ollama not found. Starting installation..."
  MAX_RETRIES=3
  RETRY_COUNT=0
  SUCCESS=false
  while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -fsSL https://ollama.com/install.sh | sh; then
      SUCCESS=true
      break
    else
      RETRY_COUNT=$((RETRY_COUNT+1))
      echo "Ollama installation failed (Attempt $RETRY_COUNT/$MAX_RETRIES). Retrying in 10 seconds..."
      sleep 10
    fi
  done
  if [ "$SUCCESS" = false ]; then
    echo "Failed to install Ollama after $MAX_RETRIES attempts."
    exit 1
  fi
fi
sudo mkdir -p /etc/systemd/system/ollama.service.d
cat <<EOF | sudo tee /etc/systemd/system/ollama.service.d/override.conf >/dev/null
[Service]
Environment="OLLAMA_HOST=0.0.0.0:$OllamaPort"
Environment="OLLAMA_KEEP_ALIVE=-1"
Environment="OLLAMA_MAX_LOADED_MODELS=1"
Environment="PATH=/usr/local/cuda/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
Environment="LD_LIBRARY_PATH=/usr/local/cuda/lib64"
EOF
sudo systemctl daemon-reload
sudo systemctl enable ollama
sudo systemctl restart ollama || sudo systemctl start ollama
'@ "ollama" "Installing Ollama runtime"

    RunWSL "ollama pull deepseek-r1:1.5b" "ollama" "Pulling deepseek-r1:1.5b model"

    Update-Step "ollama" "Installing Ollama and model assets natively in WSL" "COMPLETED" 65
} else {
    Write-Host ">>> Step 'ollama' already COMPLETED. Resuming..." -ForegroundColor Gray
}

# -------------------------
# 6. ComfyUI
# -------------------------
if (-not (Is-StepDone "comfyui")) {
    Update-Step "comfyui" "Deploying ComfyUI in WSL" "RUNNING" 67

    RunWSL @'
set -e
sudo mkdir -p /opt/nextgpu/comfy /opt/nextgpu/comfy/basedir /opt/nextgpu/comfy/basedir/custom_nodes /opt/nextgpu/comfy/basedir/models /opt/nextgpu/comfy/basedir/output
sudo chown -R nextgpu:nextgpu /opt/nextgpu/comfy
if [ ! -d /opt/nextgpu/comfy/ComfyUI/.git ]; then
  git clone https://github.com/comfyanonymous/ComfyUI.git /opt/nextgpu/comfy/ComfyUI
fi
cd /opt/nextgpu/comfy/ComfyUI
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip setuptools wheel
python -m pip install -r requirements.txt
python -m pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu124
'@ "comfyui" "Installing ComfyUI Python environment"

    RunWSL @"
set -e
cat << 'EOF' | sudo tee /etc/systemd/system/comfyui.service > /dev/null
[Unit]
Description=ComfyUI service (WSL setup)
Wants=network-online.target
After=network-online.target

[Service]
Type=simple
User=nextgpu
Group=nextgpu
WorkingDirectory=/opt/nextgpu/comfy/ComfyUI
Environment=PYTHONUNBUFFERED=1
Environment=PATH=/usr/local/cuda/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
Environment=LD_LIBRARY_PATH=/usr/local/cuda/lib64
ExecStart=/opt/nextgpu/comfy/ComfyUI/.venv/bin/python /opt/nextgpu/comfy/ComfyUI/main.py --listen 0.0.0.0 --port $ComfyPort --base-directory /opt/nextgpu/comfy/basedir
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
sudo systemctl daemon-reload
sudo systemctl enable comfyui
sudo systemctl restart comfyui || sudo systemctl start comfyui
sleep 10
"@ "comfyui" "Creating and enabling ComfyUI systemd service"

    Update-Step "comfyui" "Deploying ComfyUI natively in WSL" "COMPLETED" 80
} else {
    Write-Host ">>> Step 'comfyui' already COMPLETED. Resuming..." -ForegroundColor Gray
}

# -------------------------
# 7. STT Tool
# -------------------------
if (-not (Is-StepDone "stt_tool")) {
    Update-Step "stt_tool" "Installing NextGPU STT tool in WSL" "RUNNING" 83

    RunWSL @'
set -e

MODEL_DIR=/opt/nextgpu/stt-tool/models/small
sudo mkdir -p /opt/nextgpu/stt-tool /opt/nextgpu/stt-tool/models "$MODEL_DIR"
sudo chown -R nextgpu:nextgpu /opt/nextgpu/stt-tool

if [ ! -f /opt/nextgpu/stt-tool/requirements.txt ]; then
    if [ -d /tmp/stt-tool ]; then
      sudo rm -R /tmp/stt-tool
    fi
  git clone https://github.com/NextGPUNetwork/nextgpu-stt-tool /tmp/stt-tool
  sudo cp -rT /tmp/stt-tool /opt/nextgpu/stt-tool
  sudo chown -R nextgpu:nextgpu /opt/nextgpu/stt-tool
  rm -rf /tmp/stt-tool
fi

for url in \
  https://huggingface.co/Systran/faster-whisper-small/resolve/main/model.bin \
  https://huggingface.co/Systran/faster-whisper-small/resolve/main/config.json \
  https://huggingface.co/Systran/faster-whisper-small/resolve/main/tokenizer.json \
  https://huggingface.co/Systran/faster-whisper-small/resolve/main/vocabulary.txt
do
  filename="$(basename "$url")"
  target="$MODEL_DIR/$filename"
  part="$target.part"

  if [ -f "$target" ]; then
    echo "Skipping $filename; already downloaded."
    continue
  fi
  wget --tries=3 --show-progress -O "$part" "$url"
  mv -f "$part" "$target"
done

cd /opt/nextgpu/stt-tool
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip setuptools wheel
python -m pip install -r requirements.txt
'@ "stt_tool" "Installing NextGPU STT tool runtime and model assets"

    RunWSL @"
set -e
cat << 'EOF' | sudo tee /etc/systemd/system/nextgpu-stt-tool.service > /dev/null
[Unit]
Description=NextGPU STT Tool service
Wants=network-online.target
After=network-online.target

[Service]
Type=simple
User=nextgpu
Group=nextgpu
WorkingDirectory=/opt/nextgpu/stt-tool
Environment=HOST=0.0.0.0
Environment=PORT=$SttToolPort
Environment=PYTHONUNBUFFERED=1
ExecStart=/opt/nextgpu/stt-tool/.venv/bin/python -m src.app
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
sudo systemctl daemon-reload
sudo systemctl enable nextgpu-stt-tool
sudo systemctl restart nextgpu-stt-tool || sudo systemctl start nextgpu-stt-tool
sleep 5
"@ "stt_tool" "Creating and enabling NextGPU STT tool systemd service"

Update-Step "stt_tool" "Installing NextGPU STT tool in WSL" "COMPLETED" 88
} else {
    Write-Host ">>> Step 'stt_tool' already COMPLETED. Resuming..." -ForegroundColor Gray
}

# ---------------------------------------------------
# 8. Configure localhost proxy for Ollama and Comfy
# ---------------------------------------------------
Update-Step "wsl_service_proxies" "Configuring Windows localhost proxies for WSL services" "RUNNING" 89

$wslIp = (wsl -d nextgpu -- bash -lc "hostname -I | awk '{print `$1}'" 2>$null | Out-String).Trim()

if ([string]::IsNullOrWhiteSpace($wslIp)) {
    Update-Step "wsl_service_proxies" "Configuring Windows localhost proxies for WSL services" "FAILED" 89 `
                    "Could not resolve nextgpu WSL IP address."
}

Write-Host "Waiting for WSL services to initialize..." -ForegroundColor Yellow

Start-Sleep -Seconds 5

Write-Host "Resolved nextgpu WSL IP: $wslIp" -ForegroundColor Cyan

$proxyMappings = @(
    @{ Name = "Ollama"; Port = $OllamaPort; HealthUrl = "http://127.0.0.1:$OllamaPort/api/tags"; FirewallName = "NextGPU Ollama localhost $OllamaPort" }
    @{ Name = "ComfyUI"; Port = $ComfyPort; HealthUrl = "http://127.0.0.1:$ComfyPort"; FirewallName = "NextGPU ComfyUI localhost $ComfyPort" }
    @{ Name = "NextGPU STT Tool"; Port = $SttToolPort; HealthUrl = "http://127.0.0.1:$SttToolPort/health"; FirewallName = "NextGPU STT Tool localhost $SttToolPort" }
)

foreach ($mapping in $proxyMappings) {
    $name = $mapping.Name
    $port = $mapping.Port
    $firewallName = $mapping.FirewallName

    Write-Host "Configuring Windows localhost:$port -> WSL $wslIp`:$port for $name" -ForegroundColor Cyan

    netsh interface portproxy delete v4tov4 listenaddress=127.0.0.1 listenport=$port 2>$null | Out-Null
    netsh interface portproxy add v4tov4 listenaddress=127.0.0.1 listenport=$port connectaddress=$wslIp connectport=$port

    $existingRule = Get-NetFirewallRule -DisplayName $firewallName -ErrorAction SilentlyContinue
    if ($null -eq $existingRule) {
        New-NetFirewallRule `
                    -DisplayName $firewallName `
                    -Direction Inbound `
                    -Action Allow `
                    -Protocol TCP `
                    -LocalPort $port | Out-Null
    }
}

netsh interface portproxy delete v4tov4 listenaddress=127.0.0.1 listenport=6379 2>$null | Out-Null
$redisFirewallRule = Get-NetFirewallRule -DisplayName "NextGPU Redis localhost 6379" -ErrorAction SilentlyContinue
if ($null -ne $redisFirewallRule) {
    Remove-NetFirewallRule -DisplayName "NextGPU Redis localhost 6379"
}

curl.exe --fail --silent --show-error http://127.0.0.1:$OllamaPort/api/tags | Out-Null
if ($LASTEXITCODE -ne 0) {
    Update-Step "wsl_service_proxies" "Configuring Windows localhost proxies for WSL services" "FAILED" 89 `
                    "Ollama proxy was configured, but http://127.0.0.1:$OllamaPort/api/tags is still unreachable."
}
Write-Host "ollama-api-ready-from-windows-localhost"

$success = $false
for ($i = 0; $i -lt 20; $i++) {
    curl.exe --fail --silent http://127.0.0.1:$ComfyPort 2>$null
    if ($LASTEXITCODE -eq 0) {
        $success = $true
        break
    }
    Start-Sleep -Seconds 2
}

if (-not $success) {
    Update-Step "wsl_service_proxies" "Configuring Windows localhost proxies for WSL services" "FAILED" 89 `
                   "ComfyUI proxy was configured, but http://127.0.0.1:$ComfyPort is still unreachable."
}
Write-Host "comfyui-ready-from-windows-localhost"

$success = $false
for ($i = 0; $i -lt 20; $i++) {
    curl.exe --fail --silent http://127.0.0.1:$SttToolPort/health 2>$null
    if ($LASTEXITCODE -eq 0) {
        $success = $true
        break
    }
    Start-Sleep -Seconds 2
}

if (-not $success) {
    Update-Step "wsl_service_proxies" "Configuring Windows localhost proxies for WSL services" "FAILED" 89 `
                   "NextGPU STT Tool proxy was configured, but http://127.0.0.1:$SttToolPort/health is still unreachable."
}
Write-Host "stt-tool-ready-from-windows-localhost"

Update-Step "wsl_service_proxies" "Configuring Windows localhost proxies for WSL services" "COMPLETED" 92


# -------------------------
# 9. Verify services
# -------------------------
if (-not (Is-StepDone "service_verify")) {
    Update-Step "service_verify" "Verifying native AI services" "RUNNING" 94

    RunWSL "systemctl is-active --quiet ollama && echo ollama-active" "service_verify" "Validating Ollama service state"
    RunWSL "systemctl is-active --quiet nextgpu-stt-tool && echo stt-tool-active" "service_verify" "Validating NextGPU STT Tool service state"
    # RunWSL "systemctl is-active --quiet comfyui && echo comfyui-active" "service_verify" "Validating ComfyUI service state"

    RunWSL "curl -fsS http://127.0.0.1:$OllamaPort/api/tags >/dev/null && echo ollama-api-ready-inside-wsl" "service_verify" "Validating Ollama API endpoint inside WSL"
    RunWSL "curl -fsS http://127.0.0.1:$SttToolPort/health >/dev/null && echo stt-tool-ready-inside-wsl" "service_verify" "Validating NextGPU STT Tool endpoint inside WSL"
    # RunWSL "curl -fsS http://127.0.0.1:$ComfyPort >/dev/null && echo comfyui-ready-inside-wsl" "service_verify" "Validating ComfyUI endpoint inside WSL"

    curl.exe --fail --silent --show-error http://127.0.0.1:$OllamaPort/api/tags | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Update-Step "service_verify" "Verifying native AI services" "FAILED" 94 `
                        "Ollama is running inside WSL, but Windows 127.0.0.1:$OllamaPort is unreachable."
    }
    Write-Host "ollama-api-ready-from-windows-localhost"

    curl.exe --fail --silent --show-error http://127.0.0.1:$ComfyPort | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Update-Step "service_verify" "Verifying native AI services" "FAILED" 94 `
                        "ComfyUI is running inside WSL, but Windows 127.0.0.1:$ComfyPort is unreachable."
    }
    Write-Host "comfyui-ready-from-windows-localhost"

    curl.exe --fail --silent --show-error http://127.0.0.1:$SttToolPort/health | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Update-Step "service_verify" "Verifying native AI services" "FAILED" 94 `
                        "NextGPU STT Tool is running inside WSL, but Windows 127.0.0.1:$SttToolPort/health is unreachable."
    }
    Write-Host "stt-tool-ready-from-windows-localhost"

    Update-Step "service_verify" "Verifying native AI services" "COMPLETED" 97
} else {
    Write-Host ">>> Step 'service_verify' already COMPLETED. Resuming..." -ForegroundColor Gray
}

} else {
    Write-Host ">>> Install Profile is 'provider'. Skipping Ollama, ComfyUI, and STT-Tool proxies, and service verification..." -ForegroundColor Yellow
}

# -------------------------------------------------------
# 10. Register Windows startup task and run WSL instance
# -------------------------------------------------------
if (-not (Is-StepDone "startup_task")) {
    Update-Step "startup_task" "Registering Windows startup task" "RUNNING" 98

    $action = New-ScheduledTaskAction -Execute "wsl.exe" -Argument "-d nextgpu --exec /bin/true"
    $trigger = New-ScheduledTaskTrigger -AtLogOn
    Register-ScheduledTask -TaskName "NextGPU WSL Startup" -Action $action -Trigger $trigger -RunLevel Highest -Force

    Start-Process wsl -ArgumentList "-d nextgpu --exec /bin/true" -WindowStyle Hidden

    if ($InstallProfile -ne "provider") {
            wsl -d nextgpu -- systemctl restart ollama
            wsl -d nextgpu -- systemctl restart nextgpu-stt-tool
    }

    Update-Step "startup_task" "Registering Windows startup task" "COMPLETED" 99
} else {
    Write-Host ">>> Step 'startup_task' already COMPLETED. Resuming..." -ForegroundColor Gray
}

# ----------------------------------
# 11. All systems ready
# ----------------------------------
Update-Step "finished" "All systems ready" "COMPLETED" 100

Stop-Transcript
