# Medicion de RNF-10 (percentil 90 del flujo completo) y RNF-13 (100 solicitudes sin 5xx).
# Requiere: Tomcat con auth2fa desplegado y datos de restablecer_pruebas.sql.
# Uso: powershell -ExecutionPolicy Bypass -File scripts\medir-rendimiento.ps1

$ErrorActionPreference = "Stop"
$base = "http://localhost:8080/auth2fa"
$salida = Join-Path $PSScriptRoot "..\evidencias\metricas"
New-Item -ItemType Directory -Force $salida | Out-Null

function NuevaSesion {
    return New-Object Microsoft.PowerShell.Commands.WebRequestSession
}

# ---------- RNF-10: 20 flujos completos consecutivos ----------
# Un flujo = GET /login + POST /login (credenciales) + POST /preguntas (respuestas,
# con la redireccion a /acceso seguida). Se mide con Stopwatch y sesion nueva por flujo.
$flujos = @()
$cronometro = [System.Diagnostics.Stopwatch]::new()

for ($i = 1; $i -le 20; $i++) {
    $ws = NuevaSesion
    $tramo = @{}

    $cronometro.Restart()
    $null = Invoke-WebRequest -Uri "$base/login" -UseBasicParsing -WebSession $ws
    $cronometro.Stop(); $tramo["login"] = $cronometro.Elapsed.TotalMilliseconds

    $cronometro.Restart()
    $paso1 = Invoke-WebRequest -Uri "$base/login" -Method Post -TimeoutSec 30 `
        -Body @{ usuario = 'usuario.prueba'; password = 'Clave#2026' } -UseBasicParsing -WebSession $ws
    $cronometro.Stop(); $tramo["credenciales"] = $cronometro.Elapsed.TotalMilliseconds

    $cronometro.Restart()
    $paso2 = Invoke-WebRequest -Uri "$base/preguntas" -Method Post -TimeoutSec 30 `
        -Body @{ respuesta1 = 'Firulais'; respuesta2 = 'Cuernavaca' } -UseBasicParsing -WebSession $ws
    $cronometro.Stop(); $tramo["respuestas"] = $cronometro.Elapsed.TotalMilliseconds

    if (-not ($paso1.Content -match 'id="pregunta1"')) { throw "Flujo ${i}: no se mostraron las preguntas" }
    if (-not ($paso2.Content -match 'id="contenidoProtegido"')) { throw "Flujo ${i}: no se alcanzo la vista protegida" }

    $total = $tramo["login"] + $tramo["credenciales"] + $tramo["respuestas"]
    $flujos += [pscustomobject]@{
        ejecucion       = $i
        login_ms        = [math]::Round($tramo["login"], 1)
        credenciales_ms = [math]::Round($tramo["credenciales"], 1)
        respuestas_ms   = [math]::Round($tramo["respuestas"], 1)
        total_ms        = [math]::Round($total, 1)
    }
}

$flujos | Export-Csv -Path (Join-Path $salida "rnf10-flujos.csv") -NoTypeInformation -Encoding UTF8

$ordenados = ($flujos.total_ms | Sort-Object)
$n = $ordenados.Count
$posicionP90 = [math]::Ceiling(0.9 * $n)
$p90 = $ordenados[$posicionP90 - 1]
$resumen10 = @(
    "RNF-10 - Percentil 90 del flujo completo"
    "Definicion de ejecucion: GET /login + POST /login + POST /preguntas con redireccion a /acceso, en una sesion nueva."
    "Ejecuciones medidas: $n"
    "Metodo del percentil: rango mas cercano sobre la lista ordenada; posicion = ceil(0.90 * n) = $posicionP90"
    "Minimo (ms): $($ordenados[0])"
    "Mediana p50 (ms): $($ordenados[[math]::Ceiling(0.5 * $n) - 1])"
    "Percentil 90 (ms): $p90"
    "Maximo (ms): $($ordenados[$n - 1])"
    "Promedio (ms): $([math]::Round(($ordenados | Measure-Object -Average).Average, 1))"
    "Criterio: p90 <= 2000 ms"
    "Resultado: $(if ($p90 -le 2000) { 'CUMPLE' } else { 'NO CUMPLE' })"
)
$resumen10 | Set-Content -Path (Join-Path $salida "rnf10-resumen.txt") -Encoding UTF8
$resumen10 | ForEach-Object { $_ }

# ---------- RNF-13: 100 solicitudes con conteo de 5xx ----------
# Mezcla fija: 40 GET /login, 30 POST /login con credenciales inexistentes,
# 20 GET /acceso sin sesion (redireccion al login) y 10 GET /login?logout=1.
$plan = @()
for ($k = 0; $k -lt 40; $k++) { $plan += @{ metodo = "GET"; ruta = "/login"; cuerpo = $null } }
for ($k = 0; $k -lt 30; $k++) { $plan += @{ metodo = "POST"; ruta = "/login"; cuerpo = @{ usuario = 'usuario.inexistente'; password = 'Clave#2026' } } }
for ($k = 0; $k -lt 20; $k++) { $plan += @{ metodo = "GET"; ruta = "/acceso"; cuerpo = $null } }
for ($k = 0; $k -lt 10; $k++) { $plan += @{ metodo = "GET"; ruta = "/login?logout=1"; cuerpo = $null } }

$solicitudes = @()
$ws = NuevaSesion
$cronometro.Restart()
for ($i = 0; $i -lt $plan.Count; $i++) {
    $paso = $plan[$i]
    $cronometro.Restart()
    if ($paso.metodo -eq "POST") {
        $r = Invoke-WebRequest -Uri ($base + $paso.ruta) -Method Post -Body $paso.cuerpo `
            -UseBasicParsing -WebSession $ws -TimeoutSec 30
    } else {
        $r = Invoke-WebRequest -Uri ($base + $paso.ruta) -UseBasicParsing -WebSession $ws -TimeoutSec 30
    }
    $ms = $cronometro.Elapsed.TotalMilliseconds
    $solicitudes += [pscustomobject]@{
        numero    = $i + 1
        metodo    = $paso.metodo
        ruta      = $paso.ruta
        estado    = [int]$r.StatusCode
        tiempo_ms = [math]::Round($ms, 1)
    }
}
$cronometro.Stop()

$solicitudes | Export-Csv -Path (Join-Path $salida "rnf13-solicitudes.csv") -NoTypeInformation -Encoding UTF8

$grupos = $solicitudes | Group-Object estado | Sort-Object Name
$errores5xx = ($solicitudes | Where-Object { $_.estado -ge 500 }).Count
$resumen13 = @(
    "RNF-13 - 100 solicitudes HTTP sin errores 5xx"
    "Mezcla: 40 GET /login, 30 POST /login con usuario inexistente, 20 GET /acceso sin sesion, 10 GET /login?logout=1."
    "Solicitudes ejecutadas: $($solicitudes.Count)"
    ($grupos | ForEach-Object { "Respuestas HTTP $($_.Name): $($_.Count)" })
    "Errores 5xx: $errores5xx"
    "Criterio: 0 errores 5xx"
    "Resultado: $(if ($errores5xx -eq 0) { 'CUMPLE' } else { 'NO CUMPLE' })"
    "Tiempo acumulado de las 100 solicitudes (s): $([math]::Round((($solicitudes | Measure-Object -Property tiempo_ms -Sum).Sum) / 1000, 2))"
)
$resumen13 | Set-Content -Path (Join-Path $salida "rnf13-resumen.txt") -Encoding UTF8
$resumen13 | ForEach-Object { $_ }
