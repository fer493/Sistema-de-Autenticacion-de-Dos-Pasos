package mx.uacm.auth2fa.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object de la vista protegida de acceso autorizado.
 */
public class AccesoPage extends BasePage {

    private static final By CONTENIDO = By.id("contenidoProtegido");
    private static final By SALUDO = By.id("saludo");
    private static final By BOTON_SALIR = By.id("btnSalir");

    public AccesoPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public boolean estaVisible() {
        return presente(CONTENIDO);
    }

    public String obtenerSaludo() {
        return visible(SALUDO).getText();
    }

    public LoginPage cerrarSesion() {
        visible(BOTON_SALIR).click();
        return new LoginPage(driver, baseUrl);
    }
}
