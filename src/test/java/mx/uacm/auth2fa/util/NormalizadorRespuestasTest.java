package mx.uacm.auth2fa.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de normalización y de la validación de respuestas (UT-05).
 */
class NormalizadorRespuestasTest {

    @Test
    @DisplayName("UT-05: recorta, colapsa espacios, elimina acentos y pasa a minúsculas")
    void ejemplosDeLaTabla24() {
        assertEquals("firulais", NormalizadorRespuestas.normalizar("  FIRULÁIS  "));
        assertEquals("maria jose", NormalizadorRespuestas.normalizar("María   José"));
        assertEquals("nandu", NormalizadorRespuestas.normalizar("ÑANDÚ"));
        assertEquals("", NormalizadorRespuestas.normalizar("   "));
    }

    @Test
    @DisplayName("UT-05: la normalización es idempotente")
    void normalizarDosVecesNoCambiaElResultado() {
        String una = NormalizadorRespuestas.normalizar("  María   José ");
        String dos = NormalizadorRespuestas.normalizar(una);
        assertEquals(una, dos);
    }

    @Test
    @DisplayName("UT-05: una respuesta nula es un error de programación")
    void respuestaNulaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> NormalizadorRespuestas.normalizar(null));
    }

    @Test
    @DisplayName("UT-05: la respuesta normalizada debe tener entre 3 y 100 caracteres")
    void longitudesDeRespuesta() {
        assertFalse(ValidadorEntrada.esRespuestaValida("   "));
        assertFalse(ValidadorEntrada.esRespuestaValida("ab"));
        assertTrue(ValidadorEntrada.esRespuestaValida("abc"));
        assertTrue(ValidadorEntrada.esRespuestaValida("a".repeat(100)));
        assertFalse(ValidadorEntrada.esRespuestaValida("a".repeat(101)));
    }

    @Test
    @DisplayName("UT-05: una respuesta con acentos y espacios internos es válida")
    void respuestaConAcentosYEspacios() {
        assertTrue(ValidadorEntrada.esRespuestaValida("  María   José  "));
        assertTrue(ValidadorEntrada.esRespuestaValida("ÑANDÚ"));
    }

    @Test
    @DisplayName("UT-05: los caracteres de control se rechazan")
    void respuestaConCaracteresDeControl() {
        assertFalse(ValidadorEntrada.esRespuestaValida("abc\ndef"));
    }
}
