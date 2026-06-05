Write-Host "Starting frontend on http://localhost:5173 ..." -ForegroundColor Green
Set-Location "$PSScriptRoot\frontend"
if (-not (Test-Path "node_modules")) {
  Write-Host "Installing npm dependencies (first run only)..." -ForegroundColor Yellow
  npm install
}
npm run dev
