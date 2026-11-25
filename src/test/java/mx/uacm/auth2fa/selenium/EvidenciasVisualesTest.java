package mx.uacm.auth2fa.selenium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Capturas de apoyo para la revisión visual a dos resoluciones (RNF-14).
 *
 * <p>No forma parte de los 12 casos SEL: su producto son las imágenes de la
 * interfaz en {@code evidencias/capturas}. Se ejecuta con:
 * {@code mvn -Pselenium test -Dtest=EvidenciasVisualesTest}.</p>
 */
class EvidenciasVisualesTest {

    private static String baseUrl;
    private WebDriver driver;

    @BeforeAll
    static void cargarConfiguracion() throws IOException {
        Properties propiedades = new Properties();
        try (InputStream entrada = EvidenciasVisualesTest.class.getResourceAsStream("/selenium.properties")) {
            propiedades.load(entrada);
        }
        baseUrl = propiedades.getProperty("base.url");
    }

    @BeforeEach
    void abrirNavegador() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opciones = new ChromeOptions();
        opciones.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(opciones);
        driver.manage().deleteAllCookies();
    }

    @AfterEach
    void cerrarNavegador() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    @Test
    @DisplayName("Capturas a 1366 x 768 sin desbordamiento horizontal")
    void capturas1366() throws IOException {
        capturarFlujo(1366, 768, "1366x768");
    }

    @Test
    @DisplayName("Capturas a 1024 x 768 sin desbordamiento horizontal")
    void capturas1024() throws IOException {
        capturarFlujo(1024, 768, "1024x768");
    }

    private void capturarFlujo(int ancho, int alto, String etiqueta) throws IOException {
        driver.manage().window().setSize(new Dimension(ancho, alto));

        new LoginPage(driver, baseUrl).abrir();
        assertSinDesbordamiento();
        capturar("evidencias/capturas/login-" + etiqueta + ".png");

        new LoginPage(driver, baseUrl).ingresar("usuario.prueba", "Clave#2026");
        assertTrue(new PreguntasPage(driver, baseUrl).estaVisible());
        assertSinDesbordamiento();
        capturar("evidencias/capturas/preguntas-" + etiqueta + ".png");

        new PreguntasPage(driver, baseUrl).responder("Firulais", "Cuernavaca");
        AccesoPage acceso = new AccesoPage(driver, baseUrl);
        assertTrue(acceso.estaVisible());
        assertSinDesbordamiento();
        capturar("evidencias/capturas/acceso-" + etiqueta + ".png");
    }

    private void assertSinDesbordamiento() {
        Number desbordamiento = (Number) ((JavascriptExecutor) driver).executeScript(
                "return Math.max(0, document.documentElement.scrollWidth - window.innerWidth);");
        assertEquals(0L, desbordamiento.longValue(), "La página desborda horizontalmente");
        assertFalse(driver.getTitle().isBlank());
    }

    private void capturar(String ruta) throws IOException {
        Path destino = Paths.get(ruta);
        Files.createDirectories(destino.getParent());
        byte[] imagen = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        Files.write(destino, imagen);
    }
}
