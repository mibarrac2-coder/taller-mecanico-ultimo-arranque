param([int]$Puerto=8080,[string]$HostTaller='127.0.0.1')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'herramientas\Comun.ps1')
$env:TALLER_PORT=$Puerto.ToString()
$env:TALLER_HOST=$HostTaller
Push-Location $PSScriptRoot
try {
    $mavenTaller=Buscar-MavenTaller
    & $mavenTaller '-B' 'compile' 'exec:java'
    if ($LASTEXITCODE -ne 0) { throw "El proyecto terminó con código $LASTEXITCODE" }
} finally { Pop-Location }
