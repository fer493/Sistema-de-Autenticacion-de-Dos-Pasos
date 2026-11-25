package mx.uacm.auth2fa.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mx.uacm.auth2fa.modelo.PreguntaSeguridad;

/**
 * Pruebas de integración de {@link PreguntaDAOImpl} contra el esquema real.
 */
class PreguntaDAOImplTest {

    private static final long TD_01 = 1L;

    private final PreguntaDAOImpl dao = new PreguntaDAOImpl();

    @Test
    @DisplayName("listarPorUsuario devuelve dos preguntas ordenadas y sin hash")
    void listarPreguntas() {
        List<PreguntaSeguridad> preguntas = dao.listarPorUsuario(TD_01);

        assertEquals(2, preguntas.size());
        assertEquals(1, preguntas.get(0).getOrden());
        assertEquals(2, preguntas.get(1).getOrden());
        assertTrue(preguntas.get(0).getTexto().contains("mascota"));
        assertTrue(preguntas.get(1).getTexto().contains("Ciudad"));
        assertNull(preguntas.get(0).getHashRespuesta());
        assertNull(preguntas.get(1).getHashRespuesta());
    }

    @Test
    @DisplayName("obtenerHashesPorUsuario devuelve dos hashes PBKDF2 ordenados")
    void obtenerHashes() {
        List<String> hashes = dao.obtenerHashesPorUsuario(TD_01);

        assertEquals(2, hashes.size());
        assertTrue(hashes.get(0).startsWith("pbkdf2$210000$"));
        assertTrue(hashes.get(1).startsWith("pbkdf2$210000$"));
    }

    @Test
    @DisplayName("Un identificador inválido se rechaza antes de consultar")
    void identificadorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> dao.listarPorUsuario(0));
        assertThrows(IllegalArgumentException.class, () -> dao.obtenerHashesPorUsuario(0));
    }
}
