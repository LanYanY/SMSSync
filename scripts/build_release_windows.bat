@echo off
setlocal enabledelayedexpansion

set ROOT_DIR=%~dp0\..
cd /d %ROOT_DIR%\windows-client

if not exist .venv (
  py -3.10 -m venv .venv
)

call .venv\Scripts\activate.bat
python -m pip install --upgrade pip
pip install -r requirements.txt pyinstaller==6.11.1

pyinstaller -F -n SmsSyncWindows sms_sync_client.py
if errorlevel 1 exit /b 1

echo EXE built at: %ROOT_DIR%\windows-client\dist\SmsSyncWindows.exe
