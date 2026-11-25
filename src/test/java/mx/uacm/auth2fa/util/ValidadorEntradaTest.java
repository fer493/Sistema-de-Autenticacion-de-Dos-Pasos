package mx.uacm.auth2fa.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de los límites y formatos de entrada (UT-01 a UT-04).
 */
class ValidadorEntradaTest {

    @Test
    @DisplayName("UT-01: usuario de 4 y 20 caracteres es válido; 3 y 21 no")
    void usuarioEnLosLimitesDeLongitud() {
        assertTrue(ValidadorEntrada.esUsuarioValido("usua"));
        assertTrue(ValidadorEntrada.esUsuarioValido("usuario.veinti.chars"));

        assertFalse(ValidadorEntrada.esUsuarioValido("usu"));
        assertFalse(ValidadorEntrada.esUsuarioValido("usuario.veinti.charsx"));
    }

    @Test
    @DisplayName("UT-02: usuario vacío, con espacio, acento o @ es inválido; el guion bajo se acepta")
    void usuarioConCaracteresNoPermitidos() {
        assertFalse(ValidadorEntrada.esUsuarioValido(""));
        assertFalse(ValidadorEntrada.esUsuarioValido(" usuario"));
        assertFalse(ValidadorEntrada.esUsuarioValido("usuario "));
        assertFalse(ValidadorEntrada.esUsuarioValido("usuário"));
        assertFalse(ValidadorEntrada.esUsuarioValido("usuario@x"));
        assertTrue(ValidadorEntrada.esUsuarioValido("usuario_x"));
    }

    @Test
    @DisplayName("UT-03: contraseña de 8 y 64 caracteres es válida; 7 y 65 no")
    void passwordEnLosLimitesDeLongitud() {
        assertTrue(ValidadorEntrada.esPasswordValida("Aa1#aaaa"));
        assertFalse(ValidadorEntrada.esPasswordValida("Aa1#aaa"));

        String sesentaYUno = "a".repeat(61);
        assertTrue(ValidadorEntrada.esPasswordValida("Aa1#" + sesentaYUno.substring(0, 60)));
        assertFalse(ValidadorEntrada.esPasswordValida("Aa1#" + sesentaYUno));
    }

    @Test
    @DisplayName("UT-04: sin mayúscula, sin minúscula, sin dígito, sin especial o con espacio es inválido")
    void passwordSinComplejidadSuficiente() {
        assertFalse(ValidadorEntrada.esPasswordValida("aa1#aaaa"));
        assertFalse(ValidadorEntrada.esPasswordValida("AA1#AAAA"));
        assertFalse(ValidadorEntrada.esPasswordValida("Aa##aaaa"));
        assertFalse(ValidadorEntrada.esPasswordValida("Aa11aaaa"));
        assertFalse(ValidadorEntrada.esPasswordValida("Aa1 aaaa"));
    }

    @Test
    @DisplayName("Un valor nulo no es un usuario ni una contraseña válidos")
    void valoresNulosSonInvalidos() {
        assertFalse(ValidadorEntrada.esUsuarioValido(null));
        assertFalse(ValidadorEntrada.esPasswordValida(null));
        assertFalse(ValidadorEntrada.esRespuestaValida(null));
    }
}
