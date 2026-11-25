package mx.uacm.auth2fa.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mx.uacm.auth2fa.modelo.EstadoUsuario;
import mx.uacm.auth2fa.modelo.Usuario;

/**
 * Pruebas de integración de {@link UsuarioDAOImpl} contra el esquema real.
 *
 * <p>Se ejecutan con {@code mvn verify} y requieren la base {@code auth2fa}
 * con {@code restablecer_pruebas.sql}. Restablecen la cuenta 1 después de cada
 * caso para no dejar estado residual.</p>
 */
class UsuarioDAOImplTest {

    private static final long TD_01 = 1L;
    private UsuarioDAOImpl dao;

    @BeforeEach
    void preparar() {
        dao = new UsuarioDAOImpl();
        dao.reiniciarIntentos(TD_01);
    }

    @AfterEach
    void limpiar() {
        dao.reiniciarIntentos(TD_01);
    }

    @Test
    @DisplayName("buscarPorNombre devuelve la cuenta TD-01 con su estado")
    void buscarCuentaExistente() {
        Optional<Usuario> encontrado = dao.buscarPorNombre("usuario.prueba");

        assertTrue(encontrado.isPresent());
        Usuario usuario = encontrado.get();
        assertEquals(TD_01, usuario.getId());
        assertEquals("usuario.prueba", usuario.getNombreUsuario());
        assertEquals(EstadoUsuario.ACTIVO, usuario.getEstado());
        assertTrue(usuario.getHashPassword().startsWith("pbkdf2$210000$"));
    }

    @Test
    @DisplayName("buscarPorNombre devuelve vacío para una cuenta inexistente")
    void buscarCuentaInexistente() {
        assertTrue(dao.buscarPorNombre("usuario.inexistente").isEmpty());
    }

    @Test
    @DisplayName("actualizarIntentosFallidos persiste el valor indicado")
    void actualizarIntentos() {
        dao.actualizarIntentosFallidos(TD_01, 2);

        Usuario usuario = dao.buscarPorNombre("usuario.prueba").orElseThrow();
        assertEquals(2, usuario.getIntentosFallidos());
    }

    @Test
    @DisplayName("guardarBloqueoHasta persiste la fecha de desbloqueo")
    void guardarBloqueo() {
        Instant desbloqueo = Instant.now().plusSeconds(900).truncatedTo(ChronoUnit.SECONDS);

        dao.guardarBloqueoHasta(TD_01, desbloqueo);

        Usuario usuario = dao.buscarPorNombre("usuario.prueba").orElseThrow();
        assertEquals(desbloqueo, usuario.getBloqueadoHasta());
    }

    @Test
    @DisplayName("reiniciarIntentos deja el contador en cero y la fecha en nulo")
    void reiniciarIntentos() {
        dao.actualizarIntentosFallidos(TD_01, 3);
        dao.guardarBloqueoHasta(TD_01, Instant.now().plusSeconds(600));

        dao.reiniciarIntentos(TD_01);

        Usuario usuario = dao.buscarPorNombre("usuario.prueba").orElseThrow();
        assertEquals(0, usuario.getIntentosFallidos());
        assertNull(usuario.getBloqueadoHasta());
    }

    @Test
    @DisplayName("Los identificadores inválidos se rechazan antes de consultar")
    void identificadoresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> dao.actualizarIntentosFallidos(0, 1));
        assertThrows(IllegalArgumentException.class, () -> dao.guardarBloqueoHasta(0, Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> dao.reiniciarIntentos(0));
        assertThrows(IllegalArgumentException.class, () -> dao.buscarPorNombre(""));
    }
}
