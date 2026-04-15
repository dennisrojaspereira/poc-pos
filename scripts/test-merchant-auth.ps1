param(
    [string]$ApiBaseUrl = "http://localhost:8088",
    [string]$IdpBaseUrl = "http://localhost:8088",
    [string]$ClientId = "merchant-service",
    [string]$AdminUser = "admin-user",
    [string]$AdminPassword = "admin123",
    [string]$AuditorUser = "auditor-user",
    [string]$AuditorPassword = "auditor123"
)

$ErrorActionPreference = "Stop"

function Get-AccessToken {
    param(
        [string]$Username,
        [string]$Password
    )

    $response = Invoke-RestMethod `
        -Method Post `
        -Uri "$IdpBaseUrl/realms/poc-pos/protocol/openid-connect/token" `
        -Body @{
            client_id = $ClientId
            username = $Username
            password = $Password
            grant_type = "password"
        }

    return $response.access_token
}

function Invoke-AuthorizedRequest {
    param(
        [string]$Method,
        [string]$Uri,
        [string]$Token,
        [string]$Body = $null
    )

    $headers = @{ Authorization = "Bearer $Token" }
    if ($Body) {
        $headers["Content-Type"] = "application/json"
    }

    return Invoke-WebRequest -Method $Method -Uri $Uri -Headers $headers -Body $Body -UseBasicParsing
}

$adminToken = Get-AccessToken -Username $AdminUser -Password $AdminPassword
$auditorToken = Get-AccessToken -Username $AuditorUser -Password $AuditorPassword

$merchantId = "merchant-" + [guid]::NewGuid().ToString("N").Substring(0, 8)
$documentNumber = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds().ToString()
$payload = @{
    merchantId = $merchantId
    legalName = "Merchant $merchantId"
    documentNumber = $documentNumber
} | ConvertTo-Json

$createResponse = Invoke-AuthorizedRequest `
    -Method Post `
    -Uri "$ApiBaseUrl/api/merchants" `
    -Token $adminToken `
    -Body $payload

if ($createResponse.StatusCode -ne 201) {
    throw "Expected 201 from create merchant, got $($createResponse.StatusCode)"
}

$listResponse = Invoke-AuthorizedRequest `
    -Method Get `
    -Uri "$ApiBaseUrl/api/merchants" `
    -Token $auditorToken

if ($listResponse.StatusCode -ne 200) {
    throw "Expected 200 from list merchants, got $($listResponse.StatusCode)"
}

$listBody = $listResponse.Content | ConvertFrom-Json
if (-not ($listBody | Where-Object { $_.merchantId -eq $merchantId })) {
    throw "Created merchant $merchantId not found in list response"
}

Write-Host "Merchant auth flow passed."
Write-Host "Created merchant: $merchantId"
