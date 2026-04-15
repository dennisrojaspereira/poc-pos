@echo off
setlocal

cd /d "%~dp0"

where kind >nul 2>nul || (echo kind nao encontrado no PATH.& exit /b 1)
where kubectl >nul 2>nul || (echo kubectl nao encontrado no PATH.& exit /b 1)
where istioctl >nul 2>nul || (echo istioctl nao encontrado no PATH.& exit /b 1)
where docker >nul 2>nul || (echo docker nao encontrado no PATH.& exit /b 1)

echo Criando cluster kind...
kind get clusters | findstr /i "poc-pos" >nul
if errorlevel 1 (
  kind create cluster --config infra\kind\cluster-config.yaml
  if errorlevel 1 exit /b 1
)

kubectl cluster-info --context kind-poc-pos >nul 2>nul
if errorlevel 1 (
  echo Falha ao acessar o cluster kind-poc-pos.
  exit /b 1
)

echo Instalando Istio...
istioctl install --set profile=demo -y
if errorlevel 1 exit /b 1

echo Ajustando ingress gateway para NodePort...
kubectl patch svc istio-ingressgateway -n istio-system -p "{\"spec\":{\"type\":\"NodePort\",\"ports\":[{\"name\":\"status-port\",\"port\":15021,\"targetPort\":15021,\"nodePort\":30021},{\"name\":\"http2\",\"port\":80,\"targetPort\":8080,\"nodePort\":30080},{\"name\":\"https\",\"port\":443,\"targetPort\":8443,\"nodePort\":30443}]}}"
if errorlevel 1 exit /b 1

echo Instalando Argo CD...
kubectl create namespace argocd >nul 2>nul
kubectl apply --server-side --force-conflicts -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
if errorlevel 1 exit /b 1

echo Construindo imagem da API...
docker build -t poc-pos:kind .
if errorlevel 1 exit /b 1

echo Construindo imagem do merchant-service...
docker build -t poc-merchant-service:kind merchant-service
if errorlevel 1 exit /b 1

echo Carregando imagem no kind...
kind load docker-image poc-pos:kind --name poc-pos
if errorlevel 1 exit /b 1
kind load docker-image poc-merchant-service:kind --name poc-pos
if errorlevel 1 exit /b 1

echo Aplicando manifests da aplicacao...
kubectl apply -k infra\k8s\overlays\local-kind
if errorlevel 1 exit /b 1

echo Aguardando rollout da aplicacao...
kubectl rollout status deployment/postgres -n pos-system --timeout=180s
if errorlevel 1 exit /b 1
kubectl rollout status deployment/merchant-postgres -n pos-system --timeout=180s
if errorlevel 1 exit /b 1
kubectl rollout status deployment/payment-processor-mock -n pos-system --timeout=180s
if errorlevel 1 exit /b 1
kubectl rollout status deployment/keycloak -n pos-system --timeout=240s
if errorlevel 1 exit /b 1
kubectl rollout status deployment/opa -n pos-system --timeout=180s
if errorlevel 1 exit /b 1
kubectl rollout status deployment/pos-api -n pos-system --timeout=240s
if errorlevel 1 exit /b 1
kubectl rollout status deployment/merchant-service -n pos-system --timeout=240s
if errorlevel 1 exit /b 1

echo.
echo Cluster kind: kind-poc-pos
echo API via Istio ingress: http://localhost:8088
echo Merchant Service via Istio ingress: http://localhost:8088/api/merchants
echo Keycloak via Istio ingress: http://localhost:8088/realms/poc-pos
echo Argo CD UI: execute port-forward-argocd.bat e abra https://localhost:8089
echo Senha inicial do Argo CD: kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" ^| powershell -NoProfile -Command "[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String((Get-Content -Raw -)))"
