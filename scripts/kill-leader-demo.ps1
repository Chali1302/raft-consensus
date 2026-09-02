param([int]$LeaderPid, [int]$NodeNum)
$host.UI.RawUI.WindowTitle = "CONTROL - matar al lider"
Write-Host "Matando al nodo lider (nodo $NodeNum, PID $LeaderPid)..." -ForegroundColor Yellow
Stop-Process -Id $LeaderPid -Force
Remove-Item "logs\node$NodeNum.pid" -ErrorAction SilentlyContinue
$hora = Get-Date -Format "HH:mm:ss.fff"
Write-Host "Nodo $NodeNum (lider) detenido a las $hora" -ForegroundColor Red
