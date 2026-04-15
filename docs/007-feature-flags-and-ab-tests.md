# Feature Flags and A/B Tests

## Ferramenta
- `Flagsmith` como plataforma de feature flags

## Oportunidades para feature flags
- `authorize-flow`: alternar entre estratégia atual e nova estratégia de autorização
- `retry-profile`: variar política de retry e timeout para integrações externas
- `risk-engine`: ativar engine adicional de risco ou antifraude antes da autorização
- `void-eligibility-rules`: testar regras novas de elegibilidade para cancelamento

## Estratégia de integração
- o cliente ou gateway resolve a flag no `Flagsmith`
- a variante é propagada no header `X-Feature-Variant`
- a API registra métricas por `feature_variant`
- o `Grafana` compara `control` x `treatment`

## A/B no Grafana
- throughput por variante
- taxa de erro por variante
- latência p95 por variante
- impacto nas respostas observadas pelo `Istio`

## Observações
- flags devem proteger mudanças reversíveis e de baixo acoplamento
- regras críticas de idempotência e HMAC não devem ser desligadas por flag
