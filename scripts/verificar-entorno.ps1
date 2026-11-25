# Verifica los componentes necesarios para compilar, desplegar y probar Auth2FA.
# Uso: powershell -ExecutionPolicy Bypass -File scripts\verificar-entorno.ps1

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Continue"
$resultados = New-Object System.Collections.Generic.List[object]

function Registrar([string]$Componente, [string]$Estado, [string]$Detalle) {
    $resultados.Add([pscustomobject]@{
        Componente = $Componente
        Estado     = $Estado
        Detalle    = $Detalle
    })
}

# JDK 17
$java = Get-Command java -ErrorAction SilentlyContinue
if ($null -eq $java) {
    Registrar "JDK 17" "FALTA" "java no está en PATH"
} else {
    $salidaJava = (& java -version 2>&1 | Out-String).Trim()
    if ($salidaJava -match 'version "17') {
        Registrar "JDK 17" "OK" (($salidaJava -split "`r?`n")[0])
    } else {
        Registrar "JDK 17" "REVISAR" (($salidaJava -split "`r?`n")[0])
    }
}

# Maven
$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if ($null -eq $mvn) {
    Registrar "Maven" "FALTA" "mvn no está en PATH"
} else {
    $salidaMvn = (& mvn -v 2>&1 | Out-String).Trim()
    $primeraLinea = ($salidaMvn -split "`r?`n")[0]
    Registrar "Maven" "OK" $primeraLinea
}

# Tomcat 9
if ([string]::IsNullOrWhiteSpace($env:CATALINA_HOME)) {
    Registrar "Tomcat 9" "FALTA" "CATALINA_HOME no está definido"
} else {
    $startup = Join-Path $env:CATALINA_HOME "bin\startup.bat"
    $servletApi = Join-Path $env:CATALINA_HOME "lib\servlet-api.jar"
    if ((Test-Path $startup) -and (Test-Path $servletApi)) {
        Registrar "Tomcat 9" "OK" $env:CATALINA_HOME
    } else {
        Registrar "Tomcat 9" "REVISAR" "CATALINA_HOME no contiene bin\startup.bat y lib\servlet-api.jar"
    }
}

# MySQL
$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if ($null -eq $mysql) {
    Registrar "MySQL 8" "REVISAR" "mysql no está en PATH; puede usar la ruta completa del cliente"
} else {
    $salidaMysql = (& mysql --version 2>&1 | Out-String).Trim()
    Registrar "MySQL 8" "OK" $salidaMysql
}

# Configuración local
$dbProperties = Join-Path $PSScriptRoot "..\src\main\resources\db.properties"
if (Test-Path $dbProperties) {
    Registrar "db.properties" "OK" "Archivo local encontrado"
} else {
    Registrar "db.properties" "PENDIENTE" "Copie db.properties.example y ajuste sus credenciales"
}

$resultados | Format-Table -AutoSize

$faltantes = @($resultados | Where-Object { $_.Estado -eq "FALTA" }).Count
$pendientes = @($resultados | Where-Object { $_.Estado -ne "OK" }).Count

if ($faltantes -gt 0) {
    Write-Host "Hay $faltantes componente(s) faltante(s)." -ForegroundColor Red
    exit 1
}

if ($pendientes -gt 0) {
    Write-Host "Hay $pendientes punto(s) por revisar o configurar." -ForegroundColor Yellow
    exit 0
}

Write-Host "Entorno listo para compilar y desplegar." -ForegroundColor Green
exit 0
