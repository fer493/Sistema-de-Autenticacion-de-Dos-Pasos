package mx.uacm.auth2fa.selenium;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Guarda una captura y un resumen cuando un caso Selenium falla.
 *
 * <p>Se ejecuta mientras el WebDriver sigue abierto, antes del cierre de
 * {@code @AfterEach}. La evidencia queda en {@code evidencias/selenium}.</p>
 */
public class CapturaEnFallo implements TestExecutionExceptionHandler {

    private static final DateTimeFormatter MARCA =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Supplier<WebDriver> driver;
    private final Path carpeta;

    public CapturaEnFallo(Supplier<WebDriver> driver, String carpeta) {
        this.driver = driver;
        this.carpeta = Paths.get(carpeta);
    }

    @Override
    public void handleTestExecutionException(ExtensionContext contexto, Throwable excepcion)
            throws Throwable {
        WebDriver actual = driver.get();
        if (actual != null) {
            try {
                Files.createDirectories(carpeta);
                String marca = LocalDateTime.now().format(MARCA);
                String nombre = contexto.getTestMethod().map(m -> m.getName()).orElse("caso") + "-" + marca;
                byte[] imagen = ((TakesScreenshot) actual).getScreenshotAs(OutputType.BYTES);
                Files.write(carpeta.resolve(nombre + ".png"), imagen);
                String resumen = "url=" + actual.getCurrentUrl()
                        + System.lineSeparator() + "error=" + excepcion;
                Files.write(carpeta.resolve(nombre + ".txt"),
                        resumen.getBytes(StandardCharsets.UTF_8));
            } catch (IOException | RuntimeException falloCaptura) {
                // La captura no debe ocultar el fallo original del caso.
            }
        }
        throw excepcion;
    }
}
