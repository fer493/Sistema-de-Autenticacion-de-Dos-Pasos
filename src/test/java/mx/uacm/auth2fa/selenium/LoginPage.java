package mx.uacm.auth2fa.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object de la pantalla de acceso.
 */
public class LoginPage extends BasePage {

    private static final By USUARIO = By.id("usuario");
    private static final By PASSWORD = By.id("password");
    private static final By BOTON = By.id("btnIngresar");
    private static final By MENSAJE = By.id("mensaje");

    public LoginPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
    }

    public LoginPage abrir() {
        driver.get(baseUrl + "/login");
        return this;
    }

    public LoginPage ingresar(String usuario, String password) {
        escribir(USUARIO, usuario);
        escribir(PASSWORD, password);
        visible(BOTON).click();
        return this;
    }

    public String obtenerMensaje() {
        return visible(MENSAJE).getText();
    }

    public boolean estaEnLogin() {
        return driver.getCurrentUrl().contains("/login");
    }

    public boolean camposVisibles() {
        return visible(USUARIO).isDisplayed()
                && visible(PASSWORD).isDisplayed()
                && visible(BOTON).isDisplayed();
    }

    public String idElementoEnFoco() {
        return (String) ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("return document.activeElement ? document.activeElement.id : ''");
    }
}
