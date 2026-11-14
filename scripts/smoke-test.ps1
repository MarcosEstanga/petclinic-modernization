# Clean-clone smoke test: clone, build, start the four services and call them end to end.
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-test.ps1 -JavaHome C:\path\to\jdk-21
param(
    [string]$RepoPath = (Split-Path -Parent $PSScriptRoot),
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$WorkDir = (Join-Path $env:TEMP ("petclinic-smoke-" + (Get-Date -Format "yyyyMMddHHmmss")))
)

$ErrorActionPreference = "Stop"

$java = Join-Path $JavaHome "bin/java.exe"
if (-not (Test-Path $java)) { throw "JDK 21 not found at '$JavaHome'. Set JAVA_HOME or pass -JavaHome." }
# mvnw reads JAVA_HOME from the environment
$env:JAVA_HOME = $JavaHome

Write-Host "== 1/5 Cloning the repository into $WorkDir =="
git clone --quiet --branch release/1.0.0 --single-branch $RepoPath $WorkDir
Push-Location $WorkDir

$processes = @{}
try {
    Write-Host "== 2/5 Building (tests already ran in verify) =="
    & (Join-Path $WorkDir "mvnw.cmd") -B -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Build failed" }

    $jars = @{
        customers = "services/customers-service/target/customers-service-1.0.0.jar"
        visits    = "services/visits-service/target/visits-service-1.0.0.jar"
        vets      = "services/vets-service/target/vets-service-1.0.0.jar"
        gateway   = "api-gateway/target/api-gateway-1.0.0.jar"
    }
    $ports = @{ customers = 8081; visits = 8082; vets = 8083; gateway = 8080 }

    Write-Host "== 3/5 Starting the services =="
    foreach ($name in @("customers", "vets", "visits", "gateway")) {
        $logFile = Join-Path $WorkDir "$name.log"
        $processes[$name] = Start-Process -FilePath $java `
            -ArgumentList "-jar", (Join-Path $WorkDir $jars[$name]) `
            -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err" `
            -PassThru -WindowStyle Hidden
        Write-Host "   $name -> pid $($processes[$name].Id) (:$($ports[$name]))"
    }

    Write-Host "== 4/5 Waiting for /actuator/health =="
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
        if (-not $ok) { throw "$name did not become UP within 60s" }
        Write-Host "   $name UP"
    }

    Write-Host "== 5/5 End-to-end calls =="
    $owners = Invoke-RestMethod -Uri "http://localhost:8080/api/customers/owners?lastName=fran"
    if ($owners.content.Count -ne 1) { throw "Unexpected owner search result" }
    Write-Host "   owners?lastName=fran -> $($owners.content[0].lastName) (total $($owners.totalElements))"

    $vetList = Invoke-RestMethod -Uri "http://localhost:8080/api/vets/all"
    Write-Host "   vets/all -> $($vetList.vetList.Count) vets"

    $details = Invoke-RestMethod -Uri "http://localhost:8080/api/owners/6/details"
    $visits = ($details.pets | ForEach-Object { $_.visits.Count } | Measure-Object -Sum).Sum
    Write-Host "   owners/6/details -> $($details.lastName), $($details.pets.Count) pets, $visits visits"

    $newVisitDate = (Get-Date).AddDays(10).ToString("yyyy-MM-dd")
    $body = @{ date = $newVisitDate; description = "smoke test visit" } | ConvertTo-Json
    $created = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/visits/owners/6/pets/7/visits" `
        -ContentType "application/json" -Body $body
    Write-Host "   POST visit -> id $($created.id) for $newVisitDate"

    $swagger = & curl.exe -s -L -o NUL -w "%{http_code}" "http://localhost:8080/swagger-ui.html"
    if ($swagger -ne "200") { throw "swagger-ui.html returned HTTP $swagger" }
    Write-Host "   swagger-ui -> HTTP $swagger (following redirects)"

    Write-Host ""
    Write-Host "SMOKE TEST OK"
    "SMOKE TEST OK $(Get-Date -Format s)" | Out-File -FilePath (Join-Path $WorkDir "SMOKE-RESULT.txt") -Encoding ascii
}
finally {
    foreach ($name in $processes.Keys) {
        $p = $processes[$name]
        if ($p -and -not $p.HasExited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue }
    }
    Pop-Location
}
