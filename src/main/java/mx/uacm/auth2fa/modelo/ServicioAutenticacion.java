package mx.uacm.auth2fa.modelo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import mx.uacm.auth2fa.dao.PreguntaDAO;
import mx.uacm.auth2fa.dao.UsuarioDAO;
import mx.uacm.auth2fa.util.HashUtil;
import mx.uacm.auth2fa.util.NormalizadorRespuestas;

/**
 * Reglas de autenticación de las dos etapas.
 *
 * <p>No conoce {@code HttpSession}, JSP ni códigos HTTP. Recibe los DAO por
 * constructor para poder sustituirlos por dobles en las pruebas y devuelve un
 * {@link ResultadoAutenticacion}. Los fallos técnicos se propagan como
 * excepciones del paquete DAO; los fallos del usuario se modelan como
 * resultado.</p>
 */
public class ServicioAutenticacion {

    /** Fallos consecutivos de credenciales que activan el bloqueo temporal. */
    public static final int MAX_INTENTOS_FALLIDOS = 3;

    /** Duración del bloqueo temporal. */
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private static final Logger REGISTRO = Logger.getLogger(ServicioAutenticacion.class.getName());

    private final UsuarioDAO usuarioDAO;
    private final PreguntaDAO preguntaDAO;
    private final Clock reloj;

    /**
     * Construye el servicio con el reloj del sistema.
     *
     * @param usuarioDAO  persistencia de cuentas
     * @param preguntaDAO persistencia de preguntas
     */
    public ServicioAutenticacion(UsuarioDAO usuarioDAO, PreguntaDAO preguntaDAO) {
        this(usuarioDAO, preguntaDAO, Clock.systemUTC());
    }

    /**
     * Construye el servicio con un reloj controlado.
     *
     * <p>Visible en el paquete para que las pruebas fijen el instante sin
     * modificar el código de producción.</p>
     */
    ServicioAutenticacion(UsuarioDAO usuarioDAO, PreguntaDAO preguntaDAO, Clock reloj) {
        if (usuarioDAO == null || preguntaDAO == null) {
            throw new IllegalArgumentException("Los DAO son obligatorios");
        }
        if (reloj == null) {
            throw new IllegalArgumentException("El reloj es obligatorio");
        }
        this.usuarioDAO = usuarioDAO;
        this.preguntaDAO = preguntaDAO;
        this.reloj = reloj;
    }

    /**
     * Valida usuario y contraseña y aplica la política de intentos fallidos.
     *
     * @param usuario  nombre de acceso ya validado por el controlador
     * @param password contraseña sin recortar ni normalizar
     * @return resultado de la primera etapa
     */
    public ResultadoAutenticacion autenticarCredenciales(String usuario, String password) {
        if (usuario == null || usuario.isEmpty()) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }

        Optional<Usuario> encontrado = usuarioDAO.buscarPorNombre(usuario);
        if (encontrado.isEmpty()) {
            REGISTRO.info(() -> "LOGIN_FALLO resultado=CREDENCIALES_INVALIDAS");
            return ResultadoAutenticacion.fallido(TipoResultado.CREDENCIALES_INVALIDAS, "error.credenciales");
        }

        Usuario cuenta = encontrado.get();
        Instant ahora = reloj.instant();

        if (cuenta.estaBloqueado(ahora)) {
            REGISTRO.warning(() -> "BLOQUEO_APLICADO usuarioId=" + cuenta.getId()
                    + " intentos=" + cuenta.getIntentosFallidos());
            return ResultadoAutenticacion.fallido(TipoResultado.CUENTA_BLOQUEADA, "error.bloqueo");
        }

        int intentos = cuenta.getIntentosFallidos();
        if (cuenta.tieneBloqueoVencido(ahora)) {
            // Reinicio perezoso: el bloqueo vencido se limpia en el siguiente intento.
            usuarioDAO.reiniciarIntentos(cuenta.getId());
            intentos = 0;
        }

        if (HashUtil.verificarPassword(password, cuenta.getHashPassword())) {
            if (intentos > 0) {
                usuarioDAO.reiniciarIntentos(cuenta.getId());
            }
            REGISTRO.info(() -> "LOGIN_OK usuarioId=" + cuenta.getId());
            return ResultadoAutenticacion.exitoso(TipoResultado.CREDENCIALES_OK, cuenta.getId());
        }

        int nuevosIntentos = intentos + 1;
        if (nuevosIntentos >= MAX_INTENTOS_FALLIDOS) {
            usuarioDAO.actualizarIntentosFallidos(cuenta.getId(), MAX_INTENTOS_FALLIDOS);
            Instant desbloqueo = ahora.plus(DURACION_BLOQUEO);
            usuarioDAO.guardarBloqueoHasta(cuenta.getId(), desbloqueo);
            REGISTRO.warning(() -> "BLOQUEO_APLICADO usuarioId=" + cuenta.getId()
                    + " intentos=" + MAX_INTENTOS_FALLIDOS
                    + " desbloqueo=" + desbloqueo);
            return ResultadoAutenticacion.fallido(TipoResultado.CUENTA_BLOQUEADA, "error.bloqueo");
        }

        usuarioDAO.actualizarIntentosFallidos(cuenta.getId(), nuevosIntentos);
        REGISTRO.warning(() -> "LOGIN_FALLO usuarioId=" + cuenta.getId()
                + " resultado=CREDENCIALES_INVALIDAS");
        return ResultadoAutenticacion.fallido(TipoResultado.CREDENCIALES_INVALIDAS, "error.credenciales");
    }

    /**
     * Obtiene las dos preguntas visibles de una cuenta, sin hashes.
     *
     * @param usuarioId identificador proveniente de {@code usuarioIdPendiente}
     * @return lista ordenada por posición con exactamente dos elementos
     * @throws CuentaIncompletaException si la cuenta no tiene las dos preguntas
     */
    public List<PreguntaSeguridad> obtenerPreguntas(long usuarioId) {
        if (usuarioId <= 0) {
            throw new IllegalArgumentException("El identificador de la cuenta debe ser mayor que cero");
        }
        List<PreguntaSeguridad> preguntas = preguntaDAO.listarPorUsuario(usuarioId);
        validarCuentaCompleta(usuarioId, preguntas);
        List<PreguntaSeguridad> visibles = new ArrayList<>();
        for (PreguntaSeguridad pregunta : preguntas) {
            visibles.add(pregunta.sinHash());
        }
        visibles.sort(Comparator.comparingInt(PreguntaSeguridad::getOrden));
        return visibles;
    }

    /**
     * Verifica las dos respuestas de seguridad.
     *
     * <p>No altera el contador de intentos de credenciales. Si una o ambas
     * respuestas no coinciden, el resultado es {@code RESPUESTAS_INVALIDAS}
     * sin indicar cuál falló.</p>
     *
     * @param usuarioId  identificador de la cuenta
     * @param respuesta1 primera respuesta sin normalizar
     * @param respuesta2 segunda respuesta sin normalizar
     * @return resultado de la segunda etapa
     * @throws CuentaIncompletaException si la cuenta no tiene las dos preguntas
     */
    public ResultadoAutenticacion verificarRespuestas(long usuarioId, String respuesta1, String respuesta2) {
        if (usuarioId <= 0) {
            throw new IllegalArgumentException("El identificador de la cuenta debe ser mayor que cero");
        }
        if (respuesta1 == null || respuesta2 == null) {
            throw new IllegalArgumentException("Ambas respuestas son obligatorias");
        }

        List<PreguntaSeguridad> preguntas = preguntaDAO.listarPorUsuario(usuarioId);
        validarCuentaCompleta(usuarioId, preguntas);
        List<String> hashes = preguntaDAO.obtenerHashesPorUsuario(usuarioId);
        if (hashes.size() != 2) {
            throw new CuentaIncompletaException(
                    "La cuenta " + usuarioId + " no tiene exactamente dos hashes de respuesta");
        }

        String normalizada1 = NormalizadorRespuestas.normalizar(respuesta1);
        String normalizada2 = NormalizadorRespuestas.normalizar(respuesta2);
        boolean coincide1 = HashUtil.verificarRespuesta(normalizada1, hashes.get(0));
        boolean coincide2 = HashUtil.verificarRespuesta(normalizada2, hashes.get(1));

        if (coincide1 & coincide2) {
            REGISTRO.info(() -> "RESPUESTAS_OK usuarioId=" + usuarioId);
            return ResultadoAutenticacion.exitoso(TipoResultado.RESPUESTAS_OK, usuarioId);
        }

        REGISTRO.warning(() -> "RESPUESTAS_FALLO usuarioId=" + usuarioId
                + " resultado=RESPUESTAS_INVALIDAS");
        return ResultadoAutenticacion.fallido(TipoResultado.RESPUESTAS_INVALIDAS, "error.respuestas");
    }

    private static void validarCuentaCompleta(long usuarioId, List<PreguntaSeguridad> preguntas) {
        if (preguntas == null || preguntas.size() != 2) {
            int encontradas = preguntas == null ? 0 : preguntas.size();
            throw new CuentaIncompletaException(
                    "La cuenta " + usuarioId + " tiene " + encontradas + " preguntas; se esperaban 2");
        }
        boolean orden1 = false;
        boolean orden2 = false;
        for (PreguntaSeguridad pregunta : preguntas) {
            if (pregunta.getOrden() == 1) {
                orden1 = true;
            } else if (pregunta.getOrden() == 2) {
                orden2 = true;
            }
        }
        if (!orden1 || !orden2) {
            throw new CuentaIncompletaException(
                    "La cuenta " + usuarioId + " no tiene las preguntas con orden 1 y 2");
        }
    }
}
