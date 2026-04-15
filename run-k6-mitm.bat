@echo off
setlocal

cd /d "%~dp0"

set BASE_URL=http://localhost:8080
if not "%~1"=="" set BASE_URL=%~1

where k6 >nul 2>nul
if %errorlevel%==0 goto run_local

where docker >nul 2>nul
if errorlevel 1 (
  echo Nem k6 nem docker estao disponiveis no PATH.
  exit /b 1
)

echo Rodando k6 de simulacao MITM em http://localhost:5667 ...
docker run --rm -i ^
  -p 5667:5667 ^
  -e BASE_URL=%BASE_URL% ^
  -e HMAC_SECRET=change-me ^
  -e K6_WEB_DASHBOARD=true ^
  -e K6_WEB_DASHBOARD_PORT=5667 ^
  -e K6_WEB_DASHBOARD_EXPORT=/scripts/perf/k6/reports/mitm-dashboard.html ^
  -v "%cd%:/scripts" ^
  grafana/k6 run /scripts/perf/k6/mitm-simulation.js
exit /b %errorlevel%

:run_local
echo Rodando k6 de simulacao MITM local em http://localhost:5667 ...
set K6_WEB_DASHBOARD=true
set K6_WEB_DASHBOARD_PORT=5667
set K6_WEB_DASHBOARD_EXPORT=perf\k6\reports\mitm-dashboard.html
k6 run -e BASE_URL=%BASE_URL% -e HMAC_SECRET=change-me perf\k6\mitm-simulation.js
exit /b %errorlevel%
