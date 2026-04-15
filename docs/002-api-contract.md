# API Contract

## Headers obrigatórios
- `X-Timestamp`
- `X-Correlation-Id`
- `X-Signature`

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
