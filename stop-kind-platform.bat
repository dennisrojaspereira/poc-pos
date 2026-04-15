@echo off
setlocal

cd /d "%~dp0"

echo Removendo cluster kind-poc-pos...
kind delete cluster --name poc-pos
