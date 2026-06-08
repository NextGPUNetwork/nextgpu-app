Write-Host "Starting NextGPU uninstallation..." -ForegroundColor Yellow

# Stop WSL
wsl --shutdown 2>$null

# Remove WSL distro
wsl --unregister nextgpu 2>$null
Write-Host "WSL distro removed"

# Remove port proxies
netsh interface portproxy delete v4tov4 listenaddress=127.0.0.1 listenport=11434 2>$null
netsh interface portproxy delete v4tov4 listenaddress=127.0.0.1 listenport=8188 2>$null
netsh interface portproxy delete v4tov4 listenaddress=127.0.0.1 listenport=6379 2>$null
Write-Host "Port proxies removed"

# Remove firewall rules
Get-NetFirewallRule -DisplayName "NextGPU *" -ErrorAction SilentlyContinue | Remove-NetFirewallRule
Write-Host "Firewall rules removed"

# Remove scheduled task
Unregister-ScheduledTask -TaskName "NextGPU WSL Startup" -Confirm:$false -ErrorAction SilentlyContinue
Write-Host "Startup task removed"

# Remove local data
$nextGpuDir = Join-Path $env:LOCALAPPDATA "NextGPU"
if (Test-Path $nextGpuDir) {
    Remove-Item -Recurse -Force $nextGpuDir
    Write-Host "Local app data removed"
}

$wslDataDir = Join-Path $env:LOCALAPPDATA "WSL\nextgpu"
if (Test-Path $wslDataDir) {
    Remove-Item -Recurse -Force $wslDataDir
    Write-Host "WSL data directory removed"
}

Write-Host "NextGPU uninstallation complete." -ForegroundColor Green