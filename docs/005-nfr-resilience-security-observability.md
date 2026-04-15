# NFR

## Security
- HMAC
- timestamp
- correlationId
- teste negativo com assinatura inválida
- teste negativo com timestamp expirado

## Resilience
- timeout
- retry com backoff exponencial
- jitter
- circuit breaker
- bulkhead
- teste sob carga com `k6`

## Observability
- logs estruturados
- métricas
- dashboard `k6` exportado em HTML
- `Prometheus` para scraping
- `Grafana` para dashboards operacionais e A/B
- `Istio` para métricas de tráfego da service mesh
- `Tempo` para tracing distribuído
- spans por fluxo e por etapa crítica (`usecase`, `lookup`, integração externa, transição de domínio e persistência)
- métricas de duração por etapa para comparar gargalos entre `authorize`, `confirm` e `void`
- dashboards devem permitir segmentação por `feature_variant` e investigação por `correlation_id`/`traceId`

## Quality Gates
- cobertura minima de 80% nas linhas Java alteradas a cada commit, validada em `pre-commit`
- cobertura mínima de 80% para cada nova implementação
- toda mudança deve passar em `mvn test`
- toda mudança deve passar em regressão `k6`
- toda mudança deve passar em teste de carga/segurança `k6`

## Platform
- `Istio` para tráfego leste-oeste e políticas de malha
- `Argo` para entrega contínua e promoção
- `kind` como ambiente Kubernetes local
- `Flagsmith` para feature flags
