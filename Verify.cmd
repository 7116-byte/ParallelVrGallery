@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Verify.ps1" %*
set "PVG_EXIT=%ERRORLEVEL%"
pause
exit /b %PVG_EXIT%
