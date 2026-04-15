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

echo Rodando k6 regressivo via Docker com dashboard em http://localhost:5665 ...
docker run --rm -i ^
  -p 5665:5665 ^
  -e BASE_URL=%BASE_URL% ^
  -e HMAC_SECRET=change-me ^
  -e K6_WEB_DASHBOARD=true ^
  -e K6_WEB_DASHBOARD_PORT=5665 ^
  -e K6_WEB_DASHBOARD_EXPORT=/scripts/perf/k6/reports/regression-dashboard.html ^
  -v "%cd%:/scripts" ^
  grafana/k6 run /scripts/perf/k6/regression.js
exit /b %errorlevel%

:run_local
echo Rodando k6 regressivo local com dashboard em http://localhost:5665 ...
set K6_WEB_DASHBOARD=true
set K6_WEB_DASHBOARD_PORT=5665
set K6_WEB_DASHBOARD_EXPORT=perf\k6\reports\regression-dashboard.html
k6 run -e BASE_URL=%BASE_URL% -e HMAC_SECRET=change-me perf\k6\regression.js
exit /b %errorlevel%
