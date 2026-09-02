param(
    [int]$N = 3
)

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

for ($i = 1; $i -le $N; $i++) {
    $pidFile = "$root\logs\node$i.pid"
    if (Test-Path $pidFile) {
        $procId = Get-Content $pidFile
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        Remove-Item $pidFile -ErrorAction SilentlyContinue
        Write-Output "Nodo $i (PID $procId) detenido"
    }
}
