param (
    [switch]$OverwriteExistingWsl = $false
)

# ---------------------------------
# Self-elevating PowerShell launcher
# ---------------------------------
if (-not ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "powershell.exe"
    $psi.Arguments = "-ExecutionPolicy Bypass -File `"$PSCommandPath`" $($args -join ' ')"
    $psi.Verb = "runas"
    try {
        [System.Diagnostics.Process]::Start($psi) | Out-Null
    } catch { exit 1 }
    exit 0
}

# Force clean text encoding WITHOUT the hidden BOM (Byte Order Mark)
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[Console]::OutputEncoding = $utf8NoBom
$OutputEncoding = $utf8NoBom

# -------------------------
# Initialization & Paths
# -------------------------
$NextGpuHome = Join-Path $env:LOCALAPPDATA "NextGPU"
New-Item -ItemType Directory -Force -Path $NextGpuHome | Out-Null

$StateFile = Join-Path $NextGpuHome "state.json"
$CredentialFile = Join-Path $NextGpuHome "wsl_credentials.txt"
$LogFile = Join-Path $NextGpuHome "install_debug.log"

# -------------------------
# RESET LOGIC FOR START FRESH
# -------------------------
# We do this BEFORE Start-Transcript so we can wipe the log file cleanly
if ($OverwriteExistingWsl) {
    Write-Host "Start Fresh requested. Resetting state and wiping old logs..." -ForegroundColor Yellow

    # Wipe state
    $resetState = [PSCustomObject]@{
        initialized = $false
        progressPercentage = 0
        currentStepName = "init"
        status = "RUNNING"
        error = $null
        steps = @{}
    }
    $resetState | ConvertTo-Json -Depth 5 | Out-File -FilePath $StateFile -Encoding utf8 -Force

    # Wipe logs
    if (Test-Path $LogFile) {
        Clear-Content $LogFile -Force
    }
}

# Now start the transcript (it will append if resuming, or start fresh if wiped above)
Start-Transcript -Path $LogFile -Append -Force

# --- ROBUST STATE FUNCTIONS ---

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

# Separated StepKey (for JSON state map) and StepDesc (for UI/Console Logs)
function Update-Step($StepKey, $StepDesc, $Status, $Progress, $ErrorMessage=$null) {
    $state = Get-State

    # FIX: ONLY update progress if it's an improvement or not a failure
    if ($Progress -gt $state.progressPercentage -or $Status -eq "COMPLETED") {
        $state.progressPercentage = $Progress
    }

    $state.currentStepName = $StepKey
    # REMOVED: $state.progressPercentage = $Progress (This was overriding the check above)

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

function Is-StepDone($StepKey) {
    $state = Get-State
    if ($state.steps -is [System.Management.Automation.PSCustomObject]) {
        return ($state.steps.$StepKey -eq "COMPLETED")
    } elseif ($state.steps -is [System.Collections.Hashtable]) {
        return ($state.steps[$StepKey] -eq "COMPLETED")
    }
    return $false
}

# --- ROBUST EXECUTION FUNCTIONS ---

function RunWSL($cmd, $StepKey, $StepDesc) {
    Write-Host "Executing command for: $StepDesc" -ForegroundColor Cyan

    $cmd | wsl -d nextgpu -- bash -c "tr -d '\r' | bash -le" 2>&1 | ForEach-Object { Write-Host $_ }

    if ($LASTEXITCODE -ne 0) {
        $errMsg = "Command failed with Exit Code $LASTEXITCODE during step '$StepDesc'"

        # Retrieve current progress from state to ensure we don't send 0 and reset the UI bar
        $state = Get-State
        $currentProgress = $state.progressPercentage

        Update-Step $StepKey $StepDesc "FAILED" $currentProgress $errMsg
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

Update-Step "init" "Initializing Setup" "RUNNING" 0

if (-not (Test-Path $CredentialFile)) {
    Update-Step "init" "Initializing Setup" "FAILED" 0 "Missing credentials file."
}
$Password = (Get-Content -Path $CredentialFile -Raw).Trim()

# -------------------------
# 0. Check, Install, or Update Docker Host (Windows)
# -------------------------
if (-not (Is-StepDone "docker_host_check")) {
    Update-Step "docker_host_check" "Verifying Docker Host Environment" "RUNNING" 5

    $dockerInstalled = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $dockerInstalled) {
        Write-Host "Downloading Docker Desktop..."
        $downloadUrl = "https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe"
        $installerPath = Join-Path $env:TEMP "DockerDesktopInstaller.exe"
        Invoke-WebRequest -Uri $downloadUrl -OutFile $installerPath -UseBasicParsing
        Start-Process -FilePath $installerPath -ArgumentList "install", "--quiet", "--accept-license", "--backend=wsl-2" -Wait
    } else {
        $wingetInstalled = Get-Command winget -ErrorAction SilentlyContinue
        if ($wingetInstalled) {
            Write-Host "Checking for Docker Desktop updates via winget. Please wait..." -ForegroundColor Yellow
            winget upgrade Docker.DockerDesktop --silent --accept-package-agreements --accept-source-agreements | ForEach-Object { Write-Host $_ }
        }
    }

    Update-Step "docker_host_check" "Starting Docker Desktop Service" "RUNNING" 8
    $dockerExe = "C:\Program Files\Docker\Docker\Docker Desktop.exe"

    if (Test-Path $dockerExe) {
        $isRunning = Get-Process "Docker Desktop" -ErrorAction SilentlyContinue
        if (-not $isRunning) {
            Start-Process -FilePath $dockerExe -WindowStyle Hidden
            $timeout = 30
            while ($timeout -gt 0) {
                $null = docker info 2>$null
                if ($LASTEXITCODE -eq 0) { break }
                Start-Sleep -Seconds 2
                $timeout--
            }
        }
    }
    Update-Step "docker_host_check" "Verifying Docker Host Environment" "COMPLETED" 10
} else {
    Write-Host ">>> Step 'docker_host_check' already COMPLETED. Resuming..." -ForegroundColor Gray
}

# -------------------------
# 1. Install Ubuntu 24.04
# -------------------------
if (-not (Is-StepDone "wsl_install")) {
    Update-Step "wsl_install" "Configuring Windows Subsystem for Linux (WSL)" "RUNNING" 15
    wsl --update 2>&1 | Out-Null

    $existing = (wsl --list --quiet) -match "nextgpu"
    if ($existing -and $OverwriteExistingWsl) {
        wsl --unregister nextgpu
    }

    if (-not $existing -or $OverwriteExistingWsl) {
        Update-Step "wsl_install" "Installing Ubuntu 24.04 Core" "RUNNING" 25
        wsl --install -d Ubuntu-24.04 --no-launch 2>&1 | Out-Null

        Write-Host "Exporting and re-importing WSL instance for portability..."
        $ExportTar = Join-Path $NextGpuHome "nextgpu.tar"
        wsl --export Ubuntu-24.04 "$ExportTar"
        wsl --unregister Ubuntu-24.04
        wsl --import nextgpu "$env:LOCALAPPDATA\WSL\nextgpu" "$ExportTar"
        Remove-Item "$ExportTar" -Force
    }

    Update-Step "wsl_install" "Configuring Secure WSL User Environment" "RUNNING" 35

    RunWSL @"
cat << 'EOF' | sudo tee /etc/wsl.conf > /dev/null
[boot]
systemd=true
[user]
default=nextgpu
EOF
"@ "wsl_install" "Configuring WSL Boot Params"

    Write-Host "Restarting WSL instance to apply Systemd configuration..." -ForegroundColor Yellow
    wsl --terminate nextgpu
    Start-Sleep -Seconds 3

    RunWSL @"
set -e
id -u nextgpu &>/dev/null || sudo useradd -m -s /bin/bash nextgpu
echo 'nextgpu:$Password' | sudo chpasswd
sudo usermod -aG sudo nextgpu
echo 'nextgpu ALL=(ALL) NOPASSWD:ALL' | sudo tee /etc/sudoers.d/nextgpu
"@ "wsl_install" "Configuring Secure WSL User Environment"

    Update-Step "wsl_install" "Configuring Windows Subsystem for Linux (WSL)" "COMPLETED" 35
} else {
    Write-Host ">>> Step 'wsl_install' already COMPLETED. Resuming..." -ForegroundColor Gray
}

# -------------------------
# 3. CUDA (Ubuntu 24.04 Specific)
# -------------------------
if (-not (Is-StepDone "cuda_setup")) {
    Update-Step "cuda_setup" "Installing NVIDIA CUDA Toolkit (Local)" "RUNNING" 45

    RunWSL @'
set -e
sudo apt update
sudo apt install -y build-essential gnupg ca-certificates
wget -q https://developer.download.nvidia.com/compute/cuda/repos/wsl-ubuntu/x86_64/cuda-wsl-ubuntu.pin
sudo mv cuda-wsl-ubuntu.pin /etc/apt/preferences.d/cuda-repository-pin-600
wget -q https://developer.download.nvidia.com/compute/cuda/12.6.3/local_installers/cuda-repo-wsl-ubuntu-12-6-local_12.6.3-1_amd64.deb
sudo dpkg -i cuda-repo-wsl-ubuntu-12-6-local_12.6.3-1_amd64.deb
sudo cp /var/cuda-repo-wsl-ubuntu-12-6-local/cuda-*-keyring.gpg /usr/share/keyrings/
sudo apt-get update
sudo apt-get -y install cuda-toolkit-12-6
'@ "cuda_setup" "Installing NVIDIA CUDA Toolkit (Local)"

    RunWSL "nvidia-smi" "cuda_setup" "Verifying NVIDIA SMI"

    Update-Step "cuda_setup" "Installing NVIDIA CUDA Toolkit (Local)" "COMPLETED" 50
} else {
    Write-Host ">>> Step 'cuda_setup' already COMPLETED. Resuming..." -ForegroundColor Gray
}

# -------------------------
# 4. Native Docker WSL
# -------------------------
if (-not (Is-StepDone "docker_wsl")) {
    Update-Step "docker_wsl" "Installing Native Docker inside WSL" "RUNNING" 60

    RunWSL @'
set -e
sudo apt update
sudo DEBIAN_FRONTEND=noninteractive apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg --yes
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu noble stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo DEBIAN_FRONTEND=noninteractive apt install -y docker-ce docker-ce-cli containerd.io
sudo usermod -aG docker nextgpu
sudo systemctl enable --now docker || sudo service docker start
'@ "docker_wsl" "Installing Native Docker inside WSL"

    Update-Step "docker_wsl" "Installing Native Docker inside WSL" "COMPLETED" 65
} else {
    Write-Host ">>> Step 'docker_wsl' already COMPLETED. Resuming..." -ForegroundColor Gray
}

# -------------------------
# 5. NVIDIA Container Toolkit
# -------------------------
if (-not (Is-StepDone "nvidia_toolkit")) {
    Update-Step "nvidia_toolkit" "Configuring NVIDIA Container Toolkit" "RUNNING" 70

    RunWSL @'
set -e
curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey | sudo gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg --yes
curl -fsSL https://nvidia.github.io/libnvidia-container/stable/deb/nvidia-container-toolkit.list | \
  sed "s#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g" | \
  sudo tee /etc/apt/sources.list.d/nvidia-container-toolkit.list
sudo apt update
sudo DEBIAN_FRONTEND=noninteractive apt install -y nvidia-container-toolkit
sudo nvidia-ctk runtime configure --runtime=docker
sudo systemctl restart docker || sudo service docker restart
'@ "nvidia_toolkit" "Configuring NVIDIA Container Toolkit"

    Write-Host "Hard resetting WSL subsystem to bind Windows GPU drivers..." -ForegroundColor Yellow
    wsl --shutdown
    Start-Sleep -Seconds 5

    Update-Step "nvidia_toolkit" "Configuring NVIDIA Container Toolkit" "COMPLETED" 75
} else {
    Write-Host ">>> Step 'nvidia_toolkit' already COMPLETED. Resuming..." -ForegroundColor Gray
}

# -------------------------
# 6. Ollama & DeepSeek
# -------------------------
if (-not (Is-StepDone "ollama_deploy")) {
    Update-Step "ollama_deploy" "Deploying Ollama & DeepSeek" "RUNNING" 80

    RunWSL "sudo systemctl start docker || sudo service docker start" "ollama_deploy" "Ensuring Docker Service is Running"
    Start-Sleep -Seconds 3

    RunWSL "rm -f ~/.docker/config.json || true" "ollama_deploy" "Cleaning User Docker Config"
    RunWSL "sudo rm -f /root/.docker/config.json || true" "ollama_deploy" "Cleaning Root Docker Config"

    $ollamaStatus = Get-WSL "docker ps -a --filter name=^ollama$ --format '{{.State}}'"

    if ($ollamaStatus -ne "running") {
        RunWSL "sudo docker rm -f ollama || true" "ollama_deploy" "Removing old Ollama container"
        RunWSL "sudo docker run -d --name ollama \
            -v ollama_storage:/root/.ollama \
            --restart unless-stopped \
            --gpus all \
            -p 11434:11434 \
            -e OLLAMA_KEEP_ALIVE=-1 \
            -e OLLAMA_MAX_LOADED_MODELS=1 \
            -e NVIDIA_VISIBLE_DEVICES=all \
            -e NVIDIA_DRIVER_CAPABILITIES=compute,utility \
            ollama/ollama:latest" "ollama_deploy" "Running Ollama container"
        RunWSL "sudo docker exec ollama ollama pull deepseek-r1:1.5b" "ollama_deploy" "Pulling Deepseek Model"
    }

    Update-Step "ollama_deploy" "Deploying Ollama & DeepSeek" "COMPLETED" 85
} else {
    Write-Host ">>> Step 'ollama_deploy' already COMPLETED. Resuming..." -ForegroundColor Gray
}

## -------------------------
## 8. ComfyUI
## -------------------------
if (-not (Is-StepDone "comfyui_deploy")) {
    Update-Step "comfyui_deploy" "Deploying ComfyUI Workspace" "RUNNING" 95

    RunWSL "sudo systemctl start docker || sudo service docker start" "comfyui_deploy" "Ensuring Docker Service is Running"

    $comfyStatus = Get-WSL "docker ps -a --filter name=^comfyui-nvidia$ --format '{{.State}}'"

    if ($comfyStatus -ne "running") {
        RunWSL "sudo docker rm -f comfyui-nvidia || true" "comfyui_deploy" "Removing old ComfyUI container"

        RunWSL "sudo mkdir -p /opt/nextgpu/comfy/run /opt/nextgpu/comfy/basedir && sudo chmod -R 777 /opt/nextgpu/comfy" "comfyui_deploy" "Setting up ComfyUI directories"

        RunWSL @'
cat <<EOF | sudo tee /opt/nextgpu/comfy/run/postvenv_script.bash > /dev/null
#!/bin/bash
if [ -f "/comfy/mnt/ComfyUI/requirements.txt" ]; then
    pip install -r /comfy/mnt/ComfyUI/requirements.txt
else
    pip install sqlalchemy alembic aiohttp
fi
EOF
sudo chmod +x /opt/nextgpu/comfy/run/postvenv_script.bash
'@ "comfyui_deploy" "Creating ComfyUI startup script"

        RunWSL "sudo docker run -d \
          --name comfyui-nvidia \
          --restart unless-stopped \
          -p 8188:8188 \
          -v /opt/nextgpu/comfy/run:/comfy/mnt \
          -v /opt/nextgpu/comfy/basedir:/basedir \
          -e WANTED_UID=1000 \
          -e WANTED_GID=1000 \
          -e BASE_DIRECTORY=/basedir \
          -e SECURITY_LEVEL=normal \
          -e NVIDIA_VISIBLE_DEVICES=all \
          -e NVIDIA_DRIVER_CAPABILITIES=all \
          -e DISABLE_UPGRADES=true \
          -e FORCE_CHOWN=true \
          --gpus all \
          mmartial/comfyui-nvidia-docker:latest" "comfyui_deploy" "Running ComfyUI container"
    }

Update-Step "comfyui_deploy" "Deploying ComfyUI Workspace" "COMPLETED" 95
}else {
    Write-Host ">>> Step 'comfyui_deploy' already COMPLETED. Resuming..." -ForegroundColor Gray
}

# -------------------------
# 9. Mark as initialized
# -------------------------
Update-Step "finished" "All systems ready" "COMPLETED" 100

Stop-Transcript