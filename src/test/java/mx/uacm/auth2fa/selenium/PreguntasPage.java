package mx.uacm.auth2fa.selenium;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object de la pantalla de preguntas de seguridad.
 */
public class PreguntasPage extends BasePage {

    private static final By PREGUNTA_1 = By.id("pregunta1");
    private static final By PREGUNTA_2 = By.id("pregunta2");
    private static final By RESPUESTA_1 = By.id("respuesta1");
    private static final By RESPUESTA_2 = By.id("respuesta2");
    private static final By BOTON = By.id("btnVerificar");
    private static final By MENSAJE = By.id("mensaje");

    public PreguntasPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public PreguntasPage responder(String respuesta1, String respuesta2) {
        escribir(RESPUESTA_1, respuesta1);
        escribir(RESPUESTA_2, respuesta2);
        visible(BOTON).click();
        return this;
    }

    public List<String> obtenerPreguntas() {
        return List.of(visible(PREGUNTA_1).getText(), visible(PREGUNTA_2).getText());
    }

    public String obtenerMensaje() {
        return visible(MENSAJE).getText();
    }

    public boolean estaVisible() {
        return presente(BOTON);
    }

    public String valorRespuesta1() {
        return visible(RESPUESTA_1).getAttribute("value");
    }

    public boolean hayAlerta() {
        try {
            driver.switchTo().alert();
            return true;
        } catch (org.openqa.selenium.NoAlertPresentException excepcion) {
            return false;
        }
    }
}
