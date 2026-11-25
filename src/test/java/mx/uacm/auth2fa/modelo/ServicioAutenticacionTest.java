package mx.uacm.auth2fa.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mx.uacm.auth2fa.dao.PreguntaDAO;
import mx.uacm.auth2fa.dao.UsuarioDAO;
import mx.uacm.auth2fa.util.HashUtil;

/**
 * Pruebas del servicio con DAO simulados y reloj fijo (UT-07 a UT-12).
 */
@ExtendWith(MockitoExtension.class)
class ServicioAutenticacionTest {

    private static final long ID = 1L;
    private static final String USUARIO = "usuario.prueba";
    private static final String PASSWORD = "Clave#2026";
    private static final Instant AHORA = Instant.parse("2026-11-21T12:00:00Z");

    @Mock
    private UsuarioDAO usuarioDAO;

    @Mock
    private PreguntaDAO preguntaDAO;

    private ServicioAutenticacion servicio() {
        return new ServicioAutenticacion(usuarioDAO, preguntaDAO, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private Usuario cuenta(String nombre, String password, int intentos, Instant bloqueo, EstadoUsuario estado) {
        return new Usuario(ID, nombre, HashUtil.hashPassword(password), estado, intentos, bloqueo);
    }

    private PreguntaSeguridad pregunta(int orden) {
        String texto = orden == 1 ? "¿Nombre de tu primera mascota?" : "¿Ciudad donde naciste?";
        return new PreguntaSeguridad(orden, ID, orden, texto, null);
    }

    @Test
    @DisplayName("UT-07: credenciales correctas devuelven CREDENCIALES_OK sin bloquear")
    void credencialesCorrectas() {
        when(usuarioDAO.buscarPorNombre(USUARIO))
                .thenReturn(Optional.of(cuenta(USUARIO, PASSWORD, 0, null, EstadoUsuario.ACTIVO)));

        ResultadoAutenticacion resultado = servicio().autenticarCredenciales(USUARIO, PASSWORD);

        assertEquals(TipoResultado.CREDENCIALES_OK, resultado.getTipo());
        assertEquals(ID, resultado.getUsuarioId());
        verify(usuarioDAO, never()).guardarBloqueoHasta(anyLong(), any(Instant.class));
        verify(usuarioDAO, never()).actualizarIntentosFallidos(anyLong(), anyInt());
        verify(usuarioDAO, never()).reiniciarIntentos(anyLong());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("UT-08: tres fallos bloquean la cuenta y el cuarto intento correcto se rechaza")
    void tresFallosBloqueanLaCuenta() {
        String hash = HashUtil.hashPassword(PASSWORD);
        Usuario primero = new Usuario(ID, "ana.lopez", hash, EstadoUsuario.ACTIVO, 0, null);
        Usuario segundo = new Usuario(ID, "ana.lopez", hash, EstadoUsuario.ACTIVO, 1, null);
        Usuario tercero = new Usuario(ID, "ana.lopez", hash, EstadoUsuario.ACTIVO, 2, null);
        Usuario bloqueado = new Usuario(ID, "ana.lopez", hash, EstadoUsuario.ACTIVO, 3,
                AHORA.plus(ServicioAutenticacion.DURACION_BLOQUEO));
        when(usuarioDAO.buscarPorNombre("ana.lopez"))
                .thenReturn(Optional.of(primero), Optional.of(segundo), Optional.of(tercero), Optional.of(bloqueado));

        ServicioAutenticacion servicio = servicio();
        assertEquals(TipoResultado.CREDENCIALES_INVALIDAS,
                servicio.autenticarCredenciales("ana.lopez", "Mala#1234").getTipo());
        assertEquals(TipoResultado.CREDENCIALES_INVALIDAS,
                servicio.autenticarCredenciales("ana.lopez", "Mala#1234").getTipo());
        assertEquals(TipoResultado.CUENTA_BLOQUEADA,
                servicio.autenticarCredenciales("ana.lopez", "Mala#1234").getTipo());
        assertEquals(TipoResultado.CUENTA_BLOQUEADA,
                servicio.autenticarCredenciales("ana.lopez", PASSWORD).getTipo());

        verify(usuarioDAO).actualizarIntentosFallidos(ID, 1);
        verify(usuarioDAO).actualizarIntentosFallidos(ID, 2);
        verify(usuarioDAO).actualizarIntentosFallidos(ID, 3);
        verify(usuarioDAO).guardarBloqueoHasta(ID, AHORA.plus(ServicioAutenticacion.DURACION_BLOQUEO));
        verify(usuarioDAO, times(3)).actualizarIntentosFallidos(eq(ID), anyInt());
    }

    @Test
    @DisplayName("UT-09: un usuario inexistente devuelve CREDENCIALES_INVALIDAS sin actualizar contadores")
    void usuarioInexistente() {
        when(usuarioDAO.buscarPorNombre(USUARIO)).thenReturn(Optional.empty());

        ResultadoAutenticacion resultado = servicio().autenticarCredenciales(USUARIO, PASSWORD);

        assertEquals(TipoResultado.CREDENCIALES_INVALIDAS, resultado.getTipo());
        assertEquals("error.credenciales", resultado.getClaveMensaje());
        verify(usuarioDAO, never()).actualizarIntentosFallidos(anyLong(), anyInt());
        verify(usuarioDAO, never()).guardarBloqueoHasta(anyLong(), any(Instant.class));
        verify(usuarioDAO, never()).reiniciarIntentos(anyLong());
    }

    @Test
    @DisplayName("UT-10: una cuenta con una sola pregunta es incompleta; dos preguntas se ordenan y no llevan hash")
    void preguntasDeLaCuenta() {
        when(preguntaDAO.listarPorUsuario(ID))
                .thenReturn(List.of(pregunta(1)))
                .thenReturn(List.of(pregunta(2), pregunta(1)));

        ServicioAutenticacion servicio = servicio();
        assertThrows(CuentaIncompletaException.class, () -> servicio.obtenerPreguntas(ID));

        List<PreguntaSeguridad> visibles = servicio.obtenerPreguntas(ID);
        assertEquals(2, visibles.size());
        assertEquals(1, visibles.get(0).getOrden());
        assertEquals(2, visibles.get(1).getOrden());
        assertNull(visibles.get(0).getHashRespuesta());
        assertNull(visibles.get(1).getHashRespuesta());
    }

    @Test
    @DisplayName("UT-11: mayúsculas, acentos y espacios coinciden con los hashes normalizados")
    void respuestasNormalizadas() {
        when(preguntaDAO.listarPorUsuario(ID)).thenReturn(List.of(pregunta(1), pregunta(2)));
        when(preguntaDAO.obtenerHashesPorUsuario(ID)).thenReturn(
                List.of(HashUtil.hashRespuesta("firulais"), HashUtil.hashRespuesta("cuernavaca")));

        ResultadoAutenticacion resultado =
                servicio().verificarRespuestas(ID, "  FIRULÁIS  ", " cuernavaca ");

        assertEquals(TipoResultado.RESPUESTAS_OK, resultado.getTipo());
        assertEquals(ID, resultado.getUsuarioId());
        verify(usuarioDAO, never()).actualizarIntentosFallidos(anyLong(), anyInt());
        verify(usuarioDAO, never()).guardarBloqueoHasta(anyLong(), any(Instant.class));
        verify(usuarioDAO, never()).reiniciarIntentos(anyLong());
    }

    @Test
    @DisplayName("UT-12: una o ambas respuestas incorrectas devuelven RESPUESTAS_INVALIDAS sin tocar el contador")
    void respuestasInvalidas() {
        when(preguntaDAO.listarPorUsuario(ID)).thenReturn(List.of(pregunta(1), pregunta(2)));
        when(preguntaDAO.obtenerHashesPorUsuario(ID)).thenReturn(
                List.of(HashUtil.hashRespuesta("firulais"), HashUtil.hashRespuesta("cuernavaca")));

        ServicioAutenticacion servicio = servicio();
        ResultadoAutenticacion unaIncorrecta = servicio.verificarRespuestas(ID, "Firulais", "OtraCiudad");
        ResultadoAutenticacion ambasIncorrectas = servicio.verificarRespuestas(ID, "Otra", "Otra2");

        assertEquals(TipoResultado.RESPUESTAS_INVALIDAS, unaIncorrecta.getTipo());
        assertEquals(TipoResultado.RESPUESTAS_INVALIDAS, ambasIncorrectas.getTipo());
        assertEquals("error.respuestas", unaIncorrecta.getClaveMensaje());
        assertEquals(unaIncorrecta.getClaveMensaje(), ambasIncorrectas.getClaveMensaje());
        verify(usuarioDAO, never()).actualizarIntentosFallidos(anyLong(), anyInt());
        verify(usuarioDAO, never()).guardarBloqueoHasta(anyLong(), any(Instant.class));
        verify(usuarioDAO, never()).reiniciarIntentos(anyLong());
    }

    @Test
    @DisplayName("UT-08B: un bloqueo vencido se reinicia antes de evaluar la credencial")
    void bloqueoVencidoSeReinicia() {
        when(usuarioDAO.buscarPorNombre(USUARIO)).thenReturn(Optional.of(
                cuenta(USUARIO, PASSWORD, 3, AHORA.minusSeconds(60), EstadoUsuario.ACTIVO)));

        ResultadoAutenticacion resultado = servicio().autenticarCredenciales(USUARIO, PASSWORD);

        assertEquals(TipoResultado.CREDENCIALES_OK, resultado.getTipo());
        verify(usuarioDAO).reiniciarIntentos(ID);
    }

    @Test
    @DisplayName("UT-08C: el estado BLOQUEADO sin fecha rechaza el acceso")
    void estadoBloqueadoSinFechaRechaza() {
        when(usuarioDAO.buscarPorNombre(USUARIO)).thenReturn(Optional.of(
                cuenta(USUARIO, PASSWORD, 0, null, EstadoUsuario.BLOQUEADO)));

        ResultadoAutenticacion resultado = servicio().autenticarCredenciales(USUARIO, PASSWORD);

        assertEquals(TipoResultado.CUENTA_BLOQUEADA, resultado.getTipo());
        assertTrue(resultado.getClaveMensaje().equals("error.bloqueo"));
        verify(usuarioDAO, never()).actualizarIntentosFallidos(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Las entradas nulas son errores de programación")
    void entradasNulasLanzanExcepcion() {
        ServicioAutenticacion servicio = servicio();
        assertThrows(IllegalArgumentException.class, () -> servicio.autenticarCredenciales(null, PASSWORD));
        assertThrows(IllegalArgumentException.class, () -> servicio.autenticarCredenciales(USUARIO, null));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerPreguntas(0));
        assertThrows(IllegalArgumentException.class, () -> servicio.verificarRespuestas(0, "a", "b"));
        assertThrows(IllegalArgumentException.class, () -> servicio.verificarRespuestas(ID, null, "b"));
    }
}
