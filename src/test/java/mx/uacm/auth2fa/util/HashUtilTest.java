package mx.uacm.auth2fa.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del hash PBKDF2 con el algoritmo real (UT-06).
 */
class HashUtilTest {

    @Test
    @DisplayName("UT-06: dos hashes de la misma contraseña son distintos por la sal")
    void dosHashesDeLaMismaPasswordSonDistintos() {
        String primero = HashUtil.hashPassword("Clave#2026");
        String segundo = HashUtil.hashPassword("Clave#2026");

        assertNotEquals(primero, segundo);
        assertTrue(primero.startsWith("pbkdf2$210000$"));
        assertTrue(segundo.startsWith("pbkdf2$210000$"));
    }

    @Test
    @DisplayName("UT-06: la verificación acepta la contraseña correcta y rechaza la incorrecta")
    void verificarPasswordCorrectaEIncorrecta() {
        String hash = HashUtil.hashPassword("Clave#2026");
        assertTrue(HashUtil.verificarPassword("Clave#2026", hash));
        assertFalse(HashUtil.verificarPassword("Clave#2027", hash));
    }

    @Test
    @DisplayName("UT-06: un hash con formato inválido devuelve falso sin lanzar excepción")
    void hashConFormatoInvalidoDevuelveFalso() {
        assertFalse(HashUtil.verificarPassword("Clave#2026", "texto-plano"));
        assertFalse(HashUtil.verificarPassword("Clave#2026", "pbkdf2$abc$sal$hash"));
        assertFalse(HashUtil.verificarPassword("Clave#2026", "pbkdf2$210000$no-es-base64$tampoco"));
        assertFalse(HashUtil.verificarPassword("Clave#2026", null));
    }

    @Test
    @DisplayName("UT-06: el hash y la verificación de respuestas funcionan en ambos sentidos")
    void hashYVerificacionDeRespuesta() {
        String hash = HashUtil.hashRespuesta("firulais");
        assertTrue(HashUtil.verificarRespuesta("firulais", hash));
        assertFalse(HashUtil.verificarRespuesta("cuernavaca", hash));
        assertFalse(HashUtil.verificarRespuesta("firulais", "pbkdf2$0$AA==$AA=="));
    }

    @Test
    @DisplayName("UT-06: una contraseña vacía es un error de programación")
    void passwordVaciaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> HashUtil.hashPassword(""));
        assertThrows(IllegalArgumentException.class, () -> HashUtil.hashPassword(null));
        assertThrows(IllegalArgumentException.class, () -> HashUtil.hashRespuesta(""));
    }
}
