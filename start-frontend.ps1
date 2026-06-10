# ============================================
# 启动前端 - 奖学金评审系统   cd frontend
# ============================================
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Starting frontend on http://localhost:5173 ..." -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan

Set-Location "$PSScriptRoot\frontend"

if (-not (Test-Path "node_modules")) {
    Write-Host "Installing npm dependencies (first run only)..." -ForegroundColor Yellow
    npm install
}

npm run dev
