@echo off
setlocal

cd /d "%~dp0"

where docker >nul 2>nul
if errorlevel 1 (
  echo Docker nao encontrado no PATH.
  exit /b 1
)

where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven nao encontrado no PATH.
  exit /b 1
)

echo Subindo dependencias locais...
docker compose -f docker-compose.local.yml up -d
if errorlevel 1 (
  echo Falha ao subir docker compose.
  exit /b 1
)

echo Aguardando PostgreSQL ficar saudavel...
:wait_postgres
for /f "delims=" %%i in ('docker inspect -f "{{.State.Health.Status}}" poc-pos-postgres 2^>nul') do set PG_HEALTH=%%i
if /i not "%PG_HEALTH%"=="healthy" (
  timeout /t 2 /nobreak >nul
  goto wait_postgres
)

set DB_URL=jdbc:postgresql://localhost:5432/poc_pos
set DB_USERNAME=postgres
set DB_PASSWORD=postgres
set PAYMENT_PROCESSOR_BASE_URL=http://localhost:8081
set HMAC_SECRET=change-me

echo.
echo Dependencias prontas.
echo PostgreSQL: localhost:5432
echo Merchant PostgreSQL: localhost:5433
echo Payment processor mock: http://localhost:8081
echo Keycloak: http://localhost:8180  ^(admin/admin^)
echo OPA: http://localhost:8181
echo Merchant Service: http://localhost:8083
echo Prometheus: http://localhost:9090
echo Grafana: http://localhost:3000  ^(admin/admin^)
echo API: http://localhost:8080
echo.
echo Iniciando Spring Boot...
mvn spring-boot:run
