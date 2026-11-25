# Configura la base de datos de Auth2FA en una instalación local de MySQL 8.
# Solicita la contraseña de root sin mostrarla, ejecuta el esquema y los datos
# de prueba, crea un usuario exclusivo para la aplicación y genera db.properties.
#
# Uso: powershell -ExecutionPolicy Bypass -File scripts\configurar-mysql.ps1

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$schema = Join-Path $projectRoot "database\esquema.sql"
$data = Join-Path $projectRoot "database\datos_prueba.sql"
$dbProperties = Join-Path $projectRoot "src\main\resources\db.properties"

$mysql = $null
if (-not [string]::IsNullOrWhiteSpace($env:MYSQL_HOME)) {
    $candidato = Join-Path $env:MYSQL_HOME "bin\mysql.exe"
    if (Test-Path $candidato) { $mysql = $candidato }
}
if ($null -eq $mysql) {
    $candidato = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    if (Test-Path $candidato) { $mysql = $candidato }
}
if ($null -eq $mysql) {
    throw "No se encontró mysql.exe. Configure MYSQL_HOME o instale MySQL 8.0."
}
if (-not (Test-Path $schema)) { throw "No se encontró database\esquema.sql" }
if (-not (Test-Path $data)) { throw "No se encontró database\datos_prueba.sql" }

Write-Host "Configuración de MySQL para Auth2FA" -ForegroundColor Cyan
Write-Host "Cliente: $mysql"
Write-Host "Se solicitará la contraseña de root. No se mostrará ni se guardará." -ForegroundColor Yellow

$secure = Read-Host "Contraseña de root de MySQL" -AsSecureString
if ($null -eq $secure -or $secure.Length -eq 0) {
    throw "La contraseña no puede estar vacía."
}

$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $rootPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}

function Ejecutar-Sql([string]$archivo) {
    $proceso = Start-Process -FilePath $mysql `
        -ArgumentList @("--default-character-set=utf8mb4", "-u", "root") `
        -RedirectStandardInput $archivo `
        -NoNewWindow -Wait -PassThru
    if ($proceso.ExitCode -ne 0) {
        throw "MySQL devolvió el código $($proceso.ExitCode) al ejecutar $archivo"
    }
}

$env:MYSQL_PWD = $rootPassword
try {
    Write-Host "Comprobando la conexión con root..."
    & $mysql --default-character-set=utf8mb4 -u root -e "SELECT 1;" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "No fue posible conectarse como root. Verifique la contraseña y que el servicio MySQL80 esté iniciado."
    }

    Write-Host "Creando esquema..."
    Ejecutar-Sql $schema

    Write-Host "Cargando datos de prueba..."
    Ejecutar-Sql $data

    $appPassword = "A2fa#" + ([guid]::NewGuid().ToString("N").Substring(0, 20))
    $sqlUsuario = @"
CREATE USER IF NOT EXISTS 'auth2fa_app'@'localhost' IDENTIFIED BY '$appPassword';
ALTER USER 'auth2fa_app'@'localhost' IDENTIFIED BY '$appPassword';
CREATE USER IF NOT EXISTS 'auth2fa_app'@'127.0.0.1' IDENTIFIED BY '$appPassword';
ALTER USER 'auth2fa_app'@'127.0.0.1' IDENTIFIED BY '$appPassword';
GRANT SELECT, INSERT, UPDATE, DELETE ON auth2fa.* TO 'auth2fa_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON auth2fa.* TO 'auth2fa_app'@'127.0.0.1';
FLUSH PRIVILEGES;
"@
    Write-Host "Creando usuario exclusivo de la aplicación..."
    $sqlUsuario | & $mysql --default-character-set=utf8mb4 -u root
    if ($LASTEXITCODE -ne 0) {
        throw "No fue posible crear el usuario auth2fa_app."
    }
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    $rootPassword = $null
}

$env:MYSQL_PWD = $appPassword
try {
    $conteo = (& $mysql --default-character-set=utf8mb4 -u auth2fa_app -N -B -e "SELECT COUNT(*) FROM auth2fa.usuarios;" 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "El usuario auth2fa_app no pudo consultar la base: $conteo"
    }
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

function Escapar-Properties([string]$valor) {
    $resultado = $valor.Replace('\', '\\')
    $resultado = $resultado.Replace("`r", '\r').Replace("`n", '\n')
    $resultado = $resultado.Replace(':', '\:').Replace('=', '\=').Replace('#', '\#').Replace('!', '\!')
    return $resultado
}

$contenido = @"
# Generado por scripts\configurar-mysql.ps1. No versionar este archivo.
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/auth2fa?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Mexico_City&characterEncoding=UTF-8
db.usuario=auth2fa_app
db.password=$(Escapar-Properties $appPassword)
"@
[System.IO.File]::WriteAllText($dbProperties, $contenido, (New-Object System.Text.UTF8Encoding($false)))

Write-Host ""
Write-Host "Base de datos configurada correctamente." -ForegroundColor Green
Write-Host "Cuentas cargadas: $conteo"
Write-Host "Usuario de la aplicación: auth2fa_app"
Write-Host "Archivo generado: src\main\resources\db.properties"
Write-Host "La contraseña de root no se guardó." -ForegroundColor Green
