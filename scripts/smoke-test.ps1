# Smoke test desde un clon limpio: build, arranque de los cuatro servicios y llamadas E2E.
# Uso: powershell -File scripts/smoke-test.ps1
param(
    [string]$RepoPath = (Split-Path -Parent $PSScriptRoot),
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$WorkDir = (Join-Path $env:TEMP ("petclinic-smoke-" + (Get-Date -Format "yyyyMMddHHmmss")))
)

$ErrorActionPreference = "Stop"
$java = Join-Path $JavaHome "bin/java.exe"
if (-not (Test-Path $java)) { throw "No se encuentra el JDK 21 en '$JavaHome'. Define JAVA_HOME." }

Write-Host "== 1/5 Clonando el repositorio en $WorkDir =="
git clone --quiet --branch release/1.0.0 --single-branch $RepoPath $WorkDir
Push-Location $WorkDir

try {
    Write-Host "== 2/5 Compilando (sin tests, ya ejecutados en verify) =="
    & (Join-Path $WorkDir "mvnw.cmd") -B -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "El build falló" }

    $jars = @{
        customers = "services/customers-service/target/customers-service-1.0.0.jar"
        visits    = "services/visits-service/target/visits-service-1.0.0.jar"
        vets      = "services/vets-service/target/vets-service-1.0.0.jar"
        gateway   = "api-gateway/target/api-gateway-1.0.0.jar"
    }
    $ports = @{ customers = 8081; visits = 8082; vets = 8083; gateway = 8080 }

    Write-Host "== 3/5 Arrancando los servicios =="
    $processes = @{}
    foreach ($name in @("customers", "vets", "visits", "gateway")) {
        $logFile = Join-Path $WorkDir "$name.log"
        $processes[$name] = Start-Process -FilePath $java -ArgumentList "-jar", (Join-Path $WorkDir $jars[$name]) `
            -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err" -PassThru -WindowStyle Hidden
        Write-Host "   $name -> pid $($processes[$name].Id) (:$($ports[$name]))"
    }

    Write-Host "== 4/5 Esperando /actuator/health =="
    foreach ($name in @("customers", "vets", "visits", "gateway")) {
        $url = "http://localhost:$($ports[$name])/actuator/health"
        $ok = $false
        for ($i = 0; $i -lt 60; $i++) {
            Start-Sleep -Milliseconds 1000
            try {
                $response = Invoke-RestMethod -Uri $url -TimeoutSec 3
                if ($response.status -eq "UP") { $ok = $true; break }
            }
            catch { }
        }
        if (-not $ok) { throw "$name no llegó a UP en 60 s" }
        Write-Host "   $name UP"
    }

    Write-Host "== 5/5 Llamadas de extremo a extremo =="
    $owners = Invoke-RestMethod -Uri "http://localhost:8080/api/customers/owners?lastName=fran"
    if ($owners.content.Count -ne 1) { throw "Búsqueda de owners inesperada" }
    Write-Host "   owners?lastName=fran -> $($owners.content[0].lastName) (total $($owners.totalElements))"

    $vetList = Invoke-RestMethod -Uri "http://localhost:8080/api/vets/all"
    Write-Host "   vets/all -> $($vetList.vetList.Count) veterinarios"

    $details = Invoke-RestMethod -Uri "http://localhost:8080/api/owners/6/details"
    $visits = ($details.pets | ForEach-Object { $_.visits.Count } | Measure-Object -Sum).Sum
    Write-Host "   owners/6/details -> $($details.lastName), $($details.pets.Count) mascotas, $visits visitas"

    $newVisitDate = (Get-Date).AddDays(10).ToString("yyyy-MM-dd")
    $body = @{ date = $newVisitDate; description = "smoke test visit" } | ConvertTo-Json
    $created = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/visits/owners/6/pets/7/visits" `
        -ContentType "application/json" -Body $body
    Write-Host "   POST visita -> id $($created.id) para $newVisitDate"

    $swagger = Invoke-WebRequest -Uri "http://localhost:8080/swagger-ui.html" -MaximumRedirection 5 -TimeoutSec 10
    Write-Host "   swagger-ui -> HTTP $($swagger.StatusCode)"

    Write-Host ""
    Write-Host "SMOKE TEST OK"
    "SMOKE TEST OK $(Get-Date -Format s)" | Out-File -FilePath (Join-Path $WorkDir "SMOKE-RESULT.txt") -Encoding utf8
}
finally {
    foreach ($name in $processes.Keys) {
        $p = $processes[$name]
        if ($p -and -not $p.HasExited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue }
    }
    Pop-Location
}
