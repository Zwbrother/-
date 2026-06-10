# ============================================
# 启动后端 - 奖学金评审系统  cd backend    mvn spring-boot:run
# ============================================
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Starting backend on http://localhost:9090 ..." -ForegroundColor Green
Write-Host "H2 Console: http://localhost:9090/h2" -ForegroundColor DarkGray
Write-Host "============================================" -ForegroundColor Cyan

Set-Location "$PSScriptRoot\backend"
mvn -DskipTests spring-boot:run
