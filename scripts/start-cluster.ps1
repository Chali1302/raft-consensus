param(
    [int]$N = 3,
    [string]$ConfigFile = "cluster-3.conf"
)

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$classpath = "$root\target\classes;$root\target\lib\*"
$configPath = "$root\cluster-configs\$ConfigFile"

if (-not (Test-Path $configPath)) {
    Write-Error "No existe el fichero de configuracion: $configPath"
    exit 1
}

New-Item -ItemType Directory -Force -Path "$root\logs" | Out-Null
New-Item -ItemType Directory -Force -Path "$root\data" | Out-Null

for ($i = 1; $i -le $N; $i++) {
    $dataDir = "$root\data\node$i"
    $outLog = "$root\logs\node$i.out.log"
    $errLog = "$root\logs\node$i.err.log"

    $proc = Start-Process java -ArgumentList @(
        "-cp", "`"$classpath`"",
        "edu.tfg.raft.server.RaftServerMain",
        "--id=$i",
        "--config=`"$configPath`"",
        "--data-dir=`"$dataDir`""
    ) -RedirectStandardOutput $outLog -RedirectStandardError $errLog -PassThru -WindowStyle Hidden

    $proc.Id | Out-File "$root\logs\node$i.pid"
    Write-Output "Nodo $i arrancado (PID $($proc.Id)), logs en $outLog"
}

Write-Output "Cluster de $N nodos lanzado. Usa scripts\stop-cluster.ps1 -N $N para pararlo."
