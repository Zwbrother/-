if (Test-Path "C:\tools\jdk\jdk-17.0.13+11") {
    $env:JAVA_HOME = "C:\tools\jdk\jdk-17.0.13+11"
    $env:MAVEN_HOME = "C:\tools\maven\apache-maven-3.9.16"
    $env:PATH = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"
    Write-Host "JAVA_HOME = $env:JAVA_HOME (using C:\tools)" -ForegroundColor Cyan
} else {
    Write-Host "Using system default JDK and Maven..." -ForegroundColor Cyan
}

Write-Host "Starting backend on http://localhost:8080 ..." -ForegroundColor Green
Set-Location "$PSScriptRoot\backend"
mvn -q -DskipTests spring-boot:run
