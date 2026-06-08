param (
    [switch]$SkipAdminCheck = $false
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
    $psi.Arguments = "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`""
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

Write-Host "Starting OpenClaw Uninstallation..." -ForegroundColor Cyan

# ---------------------------------
# WSL Uninstallation Payload
# ---------------------------------
$UninstallScript = @"
# Don't exit on error (set +e), we want to forcefully clean up everything we can
set +e

# Source NVM to ensure npm and openclaw binaries are found
export NVM_DIR="`$HOME/.nvm"
[ -s "`$NVM_DIR/nvm.sh" ] && \. "`$NVM_DIR/nvm.sh"

# Fix headless WSL user-level systemd capability restrictions
export XDG_RUNTIME_DIR=/run/user/`$(id -u)
export DBUS_SESSION_BUS_ADDRESS=unix:path=`${XDG_RUNTIME_DIR}/bus

echo "Stopping OpenClaw Gateway..."
openclaw gateway stop || true

echo "Killing remaining OpenClaw processes..."
pkill -f openclaw || true
pkill -f claw || true

echo "Uninstalling Gateway Service..."
openclaw gateway uninstall || true

echo "Cleaning up systemd services..."
systemctl --user disable --now openclaw-gateway.service || true
rm -f ~/.config/systemd/user/openclaw-gateway.service
systemctl --user daemon-reload || true

echo "Removing OpenClaw directories and files..."
rm -rf ~/.openclaw
rm -rf ~/.openclaw-*
rm -rf /tmp/openclaw*
rm -rf /var/tmp/openclaw*

echo "Uninstalling NPM package..."
npm uninstall -g openclaw || true

echo "OpenClaw uninstallation sequence completed."
"@

Write-Host "Executing uninstallation commands in WSL..." -ForegroundColor Cyan

# Pipe the script into the nextgpu distro
$UninstallScript | wsl -d nextgpu -- bash -c "tr -d '\r' | bash -l" 2>&1 | ForEach-Object { Write-Host $_ }

if ($LASTEXITCODE -ne 0) {
    Write-Host "Warning: Uninstallation finished, but some components may not have existed (Exit Code $LASTEXITCODE)." -ForegroundColor Yellow
} else {
    Write-Host "Uninstallation completed successfully!" -ForegroundColor Green
}