param(
    [Parameter(Mandatory = $true)]
    [string]$RepoUrl,
    [string]$TargetRevision = "main"
)

$templatePath = Join-Path $PSScriptRoot "infra\argocd\pos-application-template.yaml"
$renderedPath = Join-Path $env:TEMP "poc-pos-argocd-application.yaml"

$content = Get-Content -Raw $templatePath
$content = $content.Replace("__REPO_URL__", $RepoUrl)
$content = $content.Replace("__TARGET_REVISION__", $TargetRevision)
Set-Content -Path $renderedPath -Value $content -Encoding UTF8

kubectl apply -f $renderedPath
kubectl get application -n argocd poc-pos
