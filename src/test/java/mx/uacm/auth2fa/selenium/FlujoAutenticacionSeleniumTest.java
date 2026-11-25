package mx.uacm.auth2fa.selenium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Pruebas funcionales SEL-01 a SEL-12 sobre Tomcat y MySQL reales.
 *
 * <p>Requieren la aplicación desplegada en la URL de
 * {@code selenium.properties}, los datos de {@code restablecer_pruebas.sql} y
 * Google Chrome. Se ejecutan con:
 * {@code mvn -Pselenium test -Dtest=FlujoAutenticacionSeleniumTest}.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlujoAutenticacionSeleniumTest {

    private static final String MSG_01 = "Complete usuario y contraseña";
    private static final String MSG_02 = "Usuario o contraseña incorrectos";
    private static final String MSG_03 = "Demasiados intentos. Espere 15 minutos o contacte al administrador";
    private static final String MSG_04 = "Complete ambas respuestas";
    private static final String MSG_05 = "La verificación de seguridad no fue satisfactoria";
    private static final String MSG_07 = "Sesión cerrada correctamente";
    private static final String MSG_08 = "Inicie sesión para continuar";

    private static String baseUrl;
    private WebDriver driver;

    @RegisterExtension
    final CapturaEnFallo captura = new CapturaEnFallo(() -> driver, "evidencias/selenium");

    @BeforeAll
    static void cargarConfiguracion() throws IOException {
        Properties propiedades = new Properties();
        try (InputStream entrada = FlujoAutenticacionSeleniumTest.class
                .getResourceAsStream("/selenium.properties")) {
            assertNotNull(entrada, "Falta selenium.properties en el classpath de pruebas");
            propiedades.load(entrada);
        }
        baseUrl = propiedades.getProperty("base.url");
        assertNotNull(baseUrl, "Falta base.url en selenium.properties");
    }

    @BeforeEach
    void abrirNavegador() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opciones = new ChromeOptions();
        opciones.addArguments("--headless=new", "--window-size=1366,768", "--disable-gpu",
                "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(opciones);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(20));
        driver.manage().deleteAllCookies();
    }

    @AfterEach
    void cerrarNavegador(TestInfo informacion) throws IOException {
        if (driver == null) {
            return;
        }
        try {
            String nombre = informacion.getTestMethod().map(m -> m.getName()).orElse("caso");
            capturar("evidencias/capturas/" + nombre + ".png");
        } finally {
            driver.quit();
            driver = null;
        }
    }

    private void capturar(String ruta) throws IOException {
        Path destino = Paths.get(ruta);
        Files.createDirectories(destino.getParent());
        byte[] imagen = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        Files.write(destino, imagen);
    }

    private LoginPage login() {
        return new LoginPage(driver, baseUrl);
    }

    private void autenticarTd01() {
        login().abrir().ingresar("usuario.prueba", "Clave#2026");
        new PreguntasPage(driver, baseUrl).responder("Firulais", "Cuernavaca");
    }

    @Test
    @Order(1)
    @DisplayName("SEL-01: la página de acceso carga con sus campos y el foco en usuario")
    void sel01CargaDeAcceso() {
        LoginPage pagina = login().abrir();

        assertTrue(pagina.camposVisibles());
        assertTrue(pagina.estaEnLogin());
        assertEquals("usuario", pagina.idElementoEnFoco());
    }

    @Test
    @Order(2)
    @DisplayName("SEL-02: el envío vacío muestra MSG-01 y no navega a preguntas")
    void sel02CamposVacios() {
        LoginPage pagina = login().abrir().ingresar("", "");

        assertEquals(MSG_01, pagina.obtenerMensaje());
        assertTrue(pagina.estaEnLogin());
        assertFalse(new PreguntasPage(driver, baseUrl).estaVisible());
    }

    @Test
    @Order(3)
    @DisplayName("SEL-03: TD-01 muestra las dos preguntas de seguridad")
    void sel03CredencialesValidas() {
        login().abrir().ingresar("usuario.prueba", "Clave#2026");
        PreguntasPage preguntas = new PreguntasPage(driver, baseUrl);

        assertTrue(preguntas.estaVisible());
        List<String> textos = preguntas.obtenerPreguntas();
        assertEquals(2, textos.size());
        assertFalse(textos.get(0).isBlank());
        assertFalse(textos.get(1).isBlank());
        assertFalse(new AccesoPage(driver, baseUrl).estaVisible());
    }

    @Test
    @Order(4)
    @DisplayName("SEL-04: usuario inexistente y contraseña incorrecta producen MSG-02 idéntico")
    void sel04CredencialesInvalidas() {
        LoginPage pagina = login().abrir().ingresar("usuario.inexistente", "Clave#2026");
        String inexistente = pagina.obtenerMensaje();

        pagina.ingresar("usuario.prueba", "Incorrecta#1");
        String incorrecta = pagina.obtenerMensaje();

        assertEquals(MSG_02, inexistente);
        assertEquals(MSG_02, incorrecta);
        assertTrue(pagina.estaEnLogin());
    }

    @Test
    @Order(5)
    @DisplayName("SEL-05: TD-15, respuestas vacías muestran MSG-04 y no conceden acceso")
    void sel05RespuestasVacias() {
        login().abrir().ingresar("usuario.prueba", "Clave#2026");
        PreguntasPage preguntas = new PreguntasPage(driver, baseUrl).responder("", "");

        assertEquals(MSG_04, preguntas.obtenerMensaje());
        assertFalse(new AccesoPage(driver, baseUrl).estaVisible());
        assertFalse(preguntas.estaVisible() && preguntas.obtenerMensaje().isEmpty());
    }

    @Test
    @Order(6)
    @DisplayName("SEL-06: TD-07 y TD-08, una o ambas respuestas incorrectas muestran MSG-05")
    void sel06RespuestaIncorrecta() {
        login().abrir().ingresar("usuario.prueba", "Clave#2026");
        PreguntasPage preguntas = new PreguntasPage(driver, baseUrl)
                .responder("Firulais", "OtraCiudad");
        String unaIncorrecta = preguntas.obtenerMensaje();

        preguntas.responder("Otra", "Otra2");
        String ambasIncorrectas = preguntas.obtenerMensaje();

        assertEquals(MSG_05, unaIncorrecta);
        assertEquals(MSG_05, ambasIncorrectas);
        assertFalse(new AccesoPage(driver, baseUrl).estaVisible());
    }

    @Test
    @Order(7)
    @DisplayName("SEL-07: TD-01 y TD-05 completan el flujo y llegan a la vista protegida")
    void sel07FlujoCompleto() {
        autenticarTd01();
        AccesoPage acceso = new AccesoPage(driver, baseUrl);

        assertTrue(acceso.estaVisible());
        assertTrue(acceso.obtenerSaludo().contains("usuario.prueba"));
        assertTrue(driver.findElement(org.openqa.selenium.By.id("btnSalir")).isDisplayed());
    }

    @Test
    @Order(8)
    @DisplayName("SEL-08: el acceso directo sin sesión redirige al login con MSG-08")
    void sel08AccesoSinSesion() {
        driver.get(baseUrl + "/acceso");
        LoginPage pagina = login();

        assertTrue(pagina.estaEnLogin());
        assertTrue(driver.getCurrentUrl().contains("error=sesion"));
        assertEquals(MSG_08, pagina.obtenerMensaje());
    }

    @Test
    @Order(9)
    @DisplayName("SEL-09: el cierre de sesión muestra MSG-07 y la vista deja de ser accesible")
    void sel09CierreDeSesion() {
        autenticarTd01();
        AccesoPage acceso = new AccesoPage(driver, baseUrl);
        assertTrue(acceso.estaVisible());

        LoginPage login = acceso.cerrarSesion();
        assertEquals(MSG_07, login.obtenerMensaje());
        assertTrue(login.estaEnLogin());

        driver.navigate().back();
        driver.get(baseUrl + "/acceso");
        assertTrue(driver.getCurrentUrl().contains("error=sesion"));
        assertEquals(MSG_08, login.obtenerMensaje());
    }

    @Test
    @Order(10)
    @DisplayName("SEL-10: TD-26 en usuario y respuesta se muestra escapado y no ejecuta script")
    void sel10IntentoXss() {
        String carga = "<script>alert(1)</script>";
        LoginPage pagina = login().abrir().ingresar(carga, "Clave#2026");
        assertEquals("Revise el formato del usuario: de 4 a 20 caracteres, solo letras, números, "
                + "punto, guion y guion bajo", pagina.obtenerMensaje());

        pagina.ingresar("usuario.prueba", "Clave#2026");
        PreguntasPage preguntas = new PreguntasPage(driver, baseUrl).responder(carga, "Cuernavaca");

        assertEquals(MSG_05, preguntas.obtenerMensaje());
        assertEquals(carga, preguntas.valorRespuesta1());
        assertFalse(preguntas.hayAlerta());
    }

    @Test
    @Order(11)
    @DisplayName("SEL-11: TD-02 se bloquea al tercer fallo y el cuarto intento correcto se rechaza")
    void sel11BloqueoPorTresFallos() {
        LoginPage pagina = login().abrir();
        pagina.ingresar("ana.lopez", "Mala#1234");
        assertEquals(MSG_02, pagina.obtenerMensaje());
        pagina.ingresar("ana.lopez", "Mala#1234");
        assertEquals(MSG_02, pagina.obtenerMensaje());
        pagina.ingresar("ana.lopez", "Mala#1234");
        assertEquals(MSG_03, pagina.obtenerMensaje());
        pagina.ingresar("ana.lopez", "Segura#2026");
        assertEquals(MSG_03, pagina.obtenerMensaje());
        assertFalse(new PreguntasPage(driver, baseUrl).estaVisible());
    }

    @Test
    @Order(12)
    @DisplayName("SEL-12: TD-06, mayúsculas, acentos y espacios adicionales conceden el acceso")
    void sel12NormalizacionDeRespuestas() {
        login().abrir().ingresar("usuario.prueba", "Clave#2026");
        new PreguntasPage(driver, baseUrl).responder("  FIRULÁIS  ", " cuernavaca ");

        AccesoPage acceso = new AccesoPage(driver, baseUrl);
        assertTrue(acceso.estaVisible());
        assertTrue(driver.getCurrentUrl().endsWith("/acceso"));
    }
}
