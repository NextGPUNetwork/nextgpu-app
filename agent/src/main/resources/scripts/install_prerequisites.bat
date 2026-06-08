@echo off
echo Starting installation...
powershell.exe -ExecutionPolicy Bypass -File "%~dp0install_prerequisites.ps1"
pause
