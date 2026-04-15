@echo off
setlocal

cd /d "%~dp0"

set BASE_URL=http://localhost:8080
if not "%~1"=="" set BASE_URL=%~1

echo Rodando testes Maven...
mvn test
if errorlevel 1 exit /b 1

echo Rodando testes do merchant-service...
mvn -f merchant-service/pom.xml test
if errorlevel 1 exit /b 1

echo Validando health local em %BASE_URL% ...
powershell -NoProfile -Command "try { $r = Invoke-WebRequest '%BASE_URL%/actuator/health' -UseBasicParsing; if ($r.StatusCode -ne 200) { exit 1 } } catch { exit 1 }"
if errorlevel 1 (
  echo API local nao respondeu em %BASE_URL%/actuator/health
  exit /b 1
)

set K6_BASE_URL=%BASE_URL%
where k6 >nul 2>nul
if not %errorlevel%==0 set K6_BASE_URL=%BASE_URL:localhost=host.docker.internal%

echo Rodando regressao k6...
call run-k6-regression.bat %K6_BASE_URL%
if errorlevel 1 exit /b 1

echo Rodando security/pentest k6...
call run-k6-security.bat %K6_BASE_URL%
if errorlevel 1 exit /b 1

echo.
echo Todos os testes passaram.
