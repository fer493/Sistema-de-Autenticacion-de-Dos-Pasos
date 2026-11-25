# Auth2FA

Sistema de Autenticación de Dos Pasos (2FA) del proyecto final de **Aseguramiento y Calidad del Software** de la UACM, plantel Cuautepéc.

El diseño de referencia es la **Entrega 3**. La aplicación implementa el flujo obligatorio de la versión 1.0: credenciales, dos preguntas de seguridad, sesión autenticada y cierre de sesión.

## Estado

La versión 1.0 está implementada, desplegada y verificada el 21/11/2026:

| Indicador | Resultado |
|---|---|
| Compilación y empaquetado | `BUILD SUCCESS`; `target/auth2fa.war` (4,571,699 bytes) |
| Pruebas unitarias | 25 / 25 aprobadas |
| Pruebas de integración | 9 / 9 aprobadas |
| Pruebas funcionales Selenium | 12 / 12 aprobadas |
| Apoyo visual (dos resoluciones) | 2 / 2 ejecuciones |
| Cobertura de líneas, modelo + utilidades | 87.2 % (225 / 258) |
| Cobertura de líneas, servicio | 92.0 % (81 / 88) |
| RNF-10, percentil 90 del flujo | 795.4 ms (criterio ≤ 2,000 ms) |
| RNF-13, 100 solicitudes | 0 errores 5xx |
| Defectos | 2 detectados, 2 corregidos, 0 abiertos |

SHA-256 del WAR entregado:

```text
B2C0AD1B61158F4A67C7559FD67ED7E33E327911290139B27F0803DBFDF1E27C
```

El WAR se verificó byte a byte contra `target/auth2fa.war` después de desplegarlo.

## Requisitos

| Componente | Versión usada | Nota |
|---|---|---|
| JDK | 17.0.12 | Se compila con `--release 17`. |
| Maven | 3.10.1 | Genera el WAR y ejecuta las pruebas. |
| Apache Tomcat | 9.0.124 | **No usar Tomcat 10**: el proyecto usa `javax.servlet`. |
| MySQL | 8.0.46 | Base de datos `auth2fa`. |
| Google Chrome | 157.0.8103.79 | Solo para las pruebas Selenium. |

Entorno de la ejecución final: Windows 11 Pro 10.0.26100, Intel Core i7-4600M a 2.90 GHz, 7.4 GB de RAM.

Verifique la instalación:

```powershell
java -version
mvn -v
powershell -ExecutionPolicy Bypass -File scripts\verificar-entorno.ps1
```

El script revisa JDK 17, Maven, Tomcat 9, MySQL y la existencia de `db.properties`. No modifica nada.

## Base de datos

### Opción automática (recomendada)

El script solicita la contraseña de `root` sin mostrarla, crea la base `auth2fa`, carga los datos de prueba, crea el usuario exclusivo `auth2fa_app` con permisos solo sobre esa base y genera `src/main/resources/db.properties`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\configurar-mysql.ps1
```

### Opción manual

```powershell
cmd /c "mysql -u root -p < database\esquema.sql"
cmd /c "mysql -u root -p < database\datos_prueba.sql"
Copy-Item src\main\resources\db.properties.example src\main\resources\db.properties
```

Edite `db.properties` con el usuario y la contraseña locales:

```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/auth2fa?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Mexico_City&characterEncoding=UTF-8
db.usuario=auth2fa_app
db.password=SU_CONTRASEÑA
```

`db.properties` está en `.gitignore`: no se versiona porque contiene credenciales.

### Restablecer datos antes de las pruebas

`datos_prueba.sql` incluye sentencias `ALTER TABLE ... AUTO_INCREMENT`, que el usuario `auth2fa_app` no puede ejecutar (defecto D-01). Para restablecer con el usuario de la aplicación use:

```powershell
cmd /c "mysql -u auth2fa_app -p auth2fa < database\restablecer_pruebas.sql"
```

El script deja 6 usuarios y 12 preguntas, sin requerir privilegios administrativos.

## Compilar y desplegar

```powershell
mvn clean verify
Copy-Item target\auth2fa.war "$env:CATALINA_HOME\webapps\"
& "$env:CATALINA_HOME\bin\startup.bat"
```

Abra `http://localhost:8080/auth2fa/login`. Para detener Tomcat:

```powershell
& "$env:CATALINA_HOME\bin\shutdown.bat"
```

Si cambia el WAR con Tomcat en ejecución, detenga el servidor, reemplace el archivo y vuelva a iniciarlo para evitar clases o JSP en caché.

## Pruebas

| Nivel | Comando | Requiere |
|---|---|---|
| Unitarias (JUnit 5 + Mockito) | `mvn test` | Nada externo |
| Unitarias + integración de DAO | `mvn clean verify` | MySQL con datos |
| Funcionales Selenium (12 casos) | `mvn -Pselenium test -Dtest=FlujoAutenticacionSeleniumTest` | Tomcat y MySQL activos |
| Capturas visuales (2 resoluciones) | `mvn -Pselenium test -Dtest=EvidenciasVisualesTest` | Tomcat y MySQL activos |
| Mediciones RNF-10 y RNF-13 | `powershell -ExecutionPolicy Bypass -File scripts\medir-rendimiento.ps1` | Tomcat activo |

Las pruebas Selenium quedan fuera de `mvn test`: se activan con el perfil `selenium`. Las de integración se ejecutan con Failsafe en la fase `verify`.

Cada caso Selenium guarda una captura en `evidencias/capturas` con el nombre del método. Si un caso falla, la extensión `CapturaEnFallo` guarda además una imagen y un resumen con la URL y la excepción.

## Datos de prueba

Las contraseñas y respuestas son ficticias y solo sirven para el entorno local.

| Cuenta | Usuario | Contraseña | Respuestas | Uso |
|---|---|---|---|---|
| TD-01 | `usuario.prueba` | `Clave#2026` | `Firulais`, `Cuernavaca` | Flujo completo y normalización |
| TD-02 | `ana.lopez` | `Segura#2026` | `Rocky`, `Puebla` | Bloqueo por tres fallos |
| — | `cuenta.bloqueada` | `Clave#2026` | — | Bloqueo vigente |
| — | `cuenta.vencida` | `Clave#2026` | — | Bloqueo vencido y reinicio perezoso |
| — | `usua` | `Clave#2026` | — | Longitud mínima de usuario |
| — | `usuario.veinti.chars` | `Clave#2026` | — | Longitud máxima de usuario |

Consulte `database/datos_prueba.sql` para el detalle de los hashes y de las preguntas.

## Evidencias

La carpeta `evidencias/` no se versiona (está en `.gitignore`) y se entrega junto con los documentos.

| Ruta | Contenido |
|---|---|
| `evidencias/consola/mvn-verify.log` | Compilación, 25 unitarias y 9 de integración |
| `evidencias/consola/selenium-flujo.log` | 12 casos SEL y su tiempo |
| `evidencias/consola/selenium-visual.log` | 2 ejecuciones visuales |
| `evidencias/consola/verificacion-http.log` | 17 comprobaciones de sesión, caché, bloqueo y XSS |
| `evidencias/capturas/` | 12 capturas SEL y 6 visuales |
| `evidencias/reportes/` | Surefire, Failsafe y JaCoCo |
| `evidencias/metricas/` | 20 flujos, 100 solicitudes y resúmenes |
| `evidencias/entorno.txt` | Versiones, equipo, WAR y SHA-256 |

## Estructura

```text
Auth2FA/
├── database/
│   ├── esquema.sql
│   ├── datos_prueba.sql
│   └── restablecer_pruebas.sql
├── scripts/
│   ├── configurar-mysql.ps1
│   ├── verificar-entorno.ps1
│   └── medir-rendimiento.ps1
├── src/
│   ├── main/
│   │   ├── java/mx/uacm/auth2fa/
│   │   │   ├── controlador/   LoginServlet, PreguntasServlet, LogoutServlet, FiltroSesion
│   │   │   ├── modelo/        ServicioAutenticacion, entidades, resultado y excepción
│   │   │   ├── util/          ValidadorEntrada, NormalizadorRespuestas, HashUtil
│   │   │   └── dao/           interfaces, implementaciones JDBC, ConexionBD
│   │   ├── resources/         mensajes.properties, db.properties.example
│   │   └── webapp/            index.jsp, estilos.css, WEB-INF/vistas y web.xml
│   └── test/java/mx/uacm/auth2fa/
│       ├── dao/               pruebas de integración
│       ├── modelo/            pruebas del servicio
│       ├── util/              pruebas de validación, normalización y hash
│       └── selenium/          Page Objects y casos funcionales
├── evidencias/                (no versionada)
├── .gitignore
├── pom.xml
└── README.md
```

## Convenciones de la versión 1.0

- Paquetes: `mx.uacm.auth2fa.controlador`, `.modelo`, `.dao` y `.util`.
- Vistas bajo `WEB-INF/vistas` para que no se abran por URL directa; `/acceso` se publica con `<jsp-file>` y lo protege `FiltroSesion`.
- Rutas: `/login`, `/preguntas`, `/acceso` y `/logout`.
- Sesión de 30 minutos; el identificador se renueva con `changeSessionId` al completar la segunda etapa.
- Mensajes centralizados en `mensajes.properties` (catálogo MSG-01 a MSG-11).
- Contraseñas y respuestas con PBKDF2WithHmacSHA256, 210,000 iteraciones, sal de 16 bytes y clave de 32 bytes; nunca en texto plano.
- Solo el paquete `dao` importa `java.sql`; las consultas usan `PreparedStatement`.
- Los fallos de respuestas no incrementan `intentos_fallidos`; solo los de credenciales activan el bloqueo de 15 minutos.
- Interfaz neutra: Arial, fondo claro, tarjeta blanca, acento `#A40000`, sin imágenes ni JavaScript decorativo.

## Decisiones de alcance

- **Sin mejoras opcionales.** La versión 1.0 cubre RF-01 a RF-10 y RNF-01 a RNF-14. Agregar TOTP o PIN habría exigido cambiar el modelo de datos, el flujo y las pruebas a cuatro días de la fecha límite, con riesgo para lo ya verificado.
- **Sin registro de horas.** La métrica de esfuerzo por fase y rol se reporta como no disponible porque el equipo no mantuvo una bitácora horaria. No se estiman valores.
- **Cobertura por capa.** JaCoCo solo instrumenta la JVM de Maven; los servlets ejecutados por Selenium en Tomcat no alimentan `jacoco.exec`. Por eso el 60.8 % global incluye controladores con cero y debe leerse junto al 87.2 % de modelo y utilidades y al 92.0 % del servicio.

## Repositorio

El repositorio es local, sin remoto configurado; la entrega se hace con la carpeta y su historial Git. No se versionan `db.properties` ni `evidencias/`.
