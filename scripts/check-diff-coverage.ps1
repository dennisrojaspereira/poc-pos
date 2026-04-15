param(
    [int]$Threshold = 80
)

$ErrorActionPreference = "Stop"

function Get-GitOutput {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $output = & git @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw ($output -join [Environment]::NewLine)
    }

    return ,$output
}

function Get-ExecutableCoverageByLine {
    param(
        [Parameter(Mandatory = $true)]
        [xml]$JacocoXml
    )

    $coverageByFile = @{}

    foreach ($packageNode in $JacocoXml.report.package) {
        $packageName = [string]$packageNode.name

        foreach ($sourceFileNode in $packageNode.sourcefile) {
            $relativePath = if ([string]::IsNullOrWhiteSpace($packageName)) {
                [string]$sourceFileNode.name
            } else {
                "$packageName/$($sourceFileNode.name)"
            }
            $normalizedPath = ($relativePath -replace "\\", "/")
            $lineCoverage = @{}

            foreach ($lineNode in $sourceFileNode.line) {
                $lineNumber = [int]$lineNode.nr
                $missedInstructions = [int]$lineNode.mi
                $coveredInstructions = [int]$lineNode.ci
                $missedBranches = [int]$lineNode.mb
                $coveredBranches = [int]$lineNode.cb
                $isExecutable = ($missedInstructions + $coveredInstructions + $missedBranches + $coveredBranches) -gt 0

                if (-not $isExecutable) {
                    continue
                }

                $lineCoverage[$lineNumber] = ($coveredInstructions + $coveredBranches) -gt 0
            }

            $coverageByFile[$normalizedPath] = $lineCoverage
        }
    }

    return $coverageByFile
}

function Get-AddedLinesForFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath
    )

    $diffOutput = Get-GitOutput -Arguments @("diff", "--cached", "--unified=0", "--", $FilePath)
    $addedLines = New-Object System.Collections.Generic.List[int]
    $currentNewLine = 0

    foreach ($rawLine in $diffOutput) {
        $line = [string]$rawLine
        if ($line -match '^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@') {
            $currentNewLine = [int]$matches[1]
            continue
        }

        if ($line.StartsWith('+++') -or $line.StartsWith('---') -or $line.StartsWith('diff --git') -or $line.StartsWith('index ')) {
            continue
        }

        if ($line.StartsWith('+')) {
            $addedLines.Add($currentNewLine)
            $currentNewLine++
            continue
        }

        if ($line.StartsWith('-')) {
            continue
        }

        if ($currentNewLine -gt 0) {
            $currentNewLine++
        }
    }

    return $addedLines
}

$repoRoot = ([string[]](Get-GitOutput -Arguments @("rev-parse", "--show-toplevel")))[0].Trim()
Set-Location $repoRoot

$stagedJavaFiles = @(Get-GitOutput -Arguments @("diff", "--cached", "--name-only", "--diff-filter=ACMR", "--", "*.java") |
    ForEach-Object { $_.Trim() } |
    Where-Object { $_ })

if ($stagedJavaFiles.Count -eq 0) {
    Write-Host "Pre-commit: nenhuma mudanca em arquivo .java staged. Pulando diff coverage."
    exit 0
}

Write-Host "Pre-commit: executando testes e gerando relatorio JaCoCo para validar diff coverage..."
& mvn test jacoco:report -q
if ($LASTEXITCODE -ne 0) {
    Write-Error "Pre-commit: falha ao executar 'mvn test jacoco:report'. Commit bloqueado."
    exit 1
}

$jacocoReportPath = Join-Path $repoRoot "target/site/jacoco/jacoco.xml"
if (-not (Test-Path $jacocoReportPath)) {
    Write-Error "Pre-commit: relatorio JaCoCo nao encontrado em '$jacocoReportPath'. Commit bloqueado."
    exit 1
}

[xml]$jacocoXml = Get-Content $jacocoReportPath
$coverageByFile = Get-ExecutableCoverageByLine -JacocoXml $jacocoXml

$totalExecutableLines = 0
$totalCoveredLines = 0
$filesWithoutCoverageData = New-Object System.Collections.Generic.List[string]

foreach ($filePath in $stagedJavaFiles) {
    $normalizedFilePath = ($filePath -replace "\\", "/")
    $addedLines = Get-AddedLinesForFile -FilePath $filePath

    if ($addedLines.Count -eq 0) {
        continue
    }

    if (-not $coverageByFile.ContainsKey($normalizedFilePath)) {
        $filesWithoutCoverageData.Add($normalizedFilePath)
        continue
    }

    $lineCoverage = $coverageByFile[$normalizedFilePath]
    $executableLines = $addedLines | Where-Object { $lineCoverage.ContainsKey($_) } | Sort-Object -Unique

    foreach ($lineNumber in $executableLines) {
        $totalExecutableLines++
        if ($lineCoverage[$lineNumber]) {
            $totalCoveredLines++
        }
    }
}

if ($filesWithoutCoverageData.Count -gt 0) {
    Write-Warning ("Pre-commit: arquivos staged sem dados no JaCoCo: " + ($filesWithoutCoverageData -join ", "))
}

if ($totalExecutableLines -eq 0) {
    Write-Host "Pre-commit: nenhuma linha Java executavel foi alterada. Diff coverage nao se aplica neste commit."
    exit 0
}

$coveragePercent = [math]::Round(($totalCoveredLines / $totalExecutableLines) * 100, 2)
Write-Host "Pre-commit: diff coverage = $coveragePercent% ($totalCoveredLines/$totalExecutableLines linhas executaveis cobertas)."

if ($coveragePercent -lt $Threshold) {
    Write-Error "Pre-commit: cobertura das mudancas abaixo de $Threshold%. Commit bloqueado."
    exit 1
}

Write-Host "Pre-commit: cobertura das mudancas atende ao minimo de $Threshold%."
