# API Contract

## Headers obrigatórios POS
- `X-Timestamp`
- `X-Correlation-Id`
- `X-Signature`

## Autenticação de usuários
- endpoints administrativos e de cadastro usam `Authorization: Bearer <jwt>`
- JWT emitido pelo IdP OIDC (`Keycloak`)
- roles esperadas no token:
  - `admin`
  - `operator`
  - `auditor`

## Authorize
`POST /authorize`

Body:
- `transactionId`
- `terminalId`
- `nsu`
- `amount`

Response:
- `200 OK`

## Confirm
`POST /confirm`

Body:
- `transactionId`

Response:
- `204 No Content`

## Void
`POST /void`

Body:
- `transactionId` ou `terminalId + nsu`

Response:
- `204 No Content`

## Merchant Service
Base path:
- `/api/merchants`

Endpoints iniciais:
- `POST /api/merchants`
- `GET /api/merchants`
- `GET /api/merchants/{merchantId}`

Autorização:
- `admin`: criar e consultar
- `operator`: consultar
- `auditor`: consultar
