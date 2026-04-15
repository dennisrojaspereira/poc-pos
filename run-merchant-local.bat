@echo off
setlocal

cd /d "%~dp0"

where docker >nul 2>nul
if errorlevel 1 (
  echo Docker nao encontrado no PATH.
  exit /b 1
)

echo Subindo Keycloak, OPA, banco do merchant e merchant-service...
docker compose -f docker-compose.local.yml up -d merchant-postgres keycloak opa merchant-service
if errorlevel 1 exit /b 1

echo.
echo Merchant Service: http://localhost:8083
echo Keycloak: http://localhost:8180  ^(admin/admin^)
echo OPA: http://localhost:8181
