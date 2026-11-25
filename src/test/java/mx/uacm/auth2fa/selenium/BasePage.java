package mx.uacm.auth2fa.selenium;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Base de los Page Objects: concentra el WebDriver, la espera explícita y la
 * URL base. No contiene aserciones.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait espera;
    protected final String baseUrl;

    protected BasePage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.espera = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    protected WebElement visible(By localizador) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(localizador));
    }

    protected boolean presente(By localizador) {
        try {
            return visible(localizador).isDisplayed();
        } catch (RuntimeException excepcion) {
            return false;
        }
    }

    protected void escribir(By localizador, String valor) {
        WebElement campo = visible(localizador);
        campo.clear();
        campo.sendKeys(valor);
    }

    public String urlActual() {
        return driver.getCurrentUrl();
    }
}
