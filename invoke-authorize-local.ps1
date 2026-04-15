$timestamp = [DateTimeOffset]::UtcNow.ToString("o")
$correlationId = [guid]::NewGuid().ToString()
$body = '{"transactionId":"tx-100","terminalId":"term-1","nsu":"nsu-100","amount":10.00}'
$canonical = "POST`n/authorize`n$timestamp`n$correlationId`n$body"
$hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes("change-me"))
$signatureBytes = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($canonical))
$signature = ([System.BitConverter]::ToString($signatureBytes)).Replace("-", "").ToLower()

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/authorize" `
  -ContentType "application/json" `
  -Headers @{
    "X-Timestamp" = $timestamp
    "X-Correlation-Id" = $correlationId
    "X-Signature" = $signature
  } `
  -Body $body
