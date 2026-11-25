package mx.uacm.auth2fa.controlador;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import mx.uacm.auth2fa.dao.DataAccessException;
import mx.uacm.auth2fa.dao.PreguntaDAOImpl;
import mx.uacm.auth2fa.dao.UsuarioDAOImpl;
import mx.uacm.auth2fa.modelo.CuentaIncompletaException;
import mx.uacm.auth2fa.modelo.PreguntaSeguridad;
import mx.uacm.auth2fa.modelo.ResultadoAutenticacion;
import mx.uacm.auth2fa.modelo.ServicioAutenticacion;
import mx.uacm.auth2fa.modelo.TipoResultado;
import mx.uacm.auth2fa.util.NormalizadorRespuestas;
import mx.uacm.auth2fa.util.ValidadorEntrada;

/**
 * Controlador de la segunda etapa: preguntas de seguridad.
 *
 * <p>Sin {@code usuarioIdPendiente} no consulta la base de datos. Al verificar
 * ambas respuestas crea la sesión autenticada, renueva el identificador y
 * redirige a la vista protegida con el patrón POST-Redirect-GET.</p>
 */
public class PreguntasServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger REGISTRO = Logger.getLogger(PreguntasServlet.class.getName());

    private static final String VISTA_PREGUNTAS = "/WEB-INF/vistas/preguntas.jsp";
    private static final String VISTA_ERROR = "/WEB-INF/vistas/error.jsp";
    private static final String RUTA_LOGIN = "/login";
    private static final String RUTA_ACCESO = "/acceso";

    private static final String ATRIBUTO_PENDIENTE = "usuarioIdPendiente";
    private static final String ATRIBUTO_AUTENTICADO = "usuarioAutenticado";
    private static final String ATRIBUTO_USUARIO_ID = "usuarioId";

    private transient ServicioAutenticacion servicio;

    @Override
    public void init() throws ServletException {
        servicio = new ServicioAutenticacion(new UsuarioDAOImpl(), new PreguntaDAOImpl());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long pendiente = obtenerPendiente(request);
        if (pendiente == null) {
            response.sendRedirect(request.getContextPath() + RUTA_LOGIN + "?error=sesion");
            return;
        }

        try {
            mostrarPreguntas(request, response, pendiente);
        } catch (CuentaIncompletaException excepcion) {
            mostrarErrorTecnico(request, response, excepcion, "CUENTA_INCOMPLETA");
        } catch (DataAccessException excepcion) {
            mostrarErrorTecnico(request, response, excepcion, "DATA_ACCESS_ERROR");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        Long pendiente = obtenerPendiente(request);
        if (pendiente == null) {
            response.sendRedirect(request.getContextPath() + RUTA_LOGIN + "?error=sesion");
            return;
        }

        String respuesta1 = request.getParameter("respuesta1");
        String respuesta2 = request.getParameter("respuesta2");
        request.setAttribute("respuesta1Ingresada", respuesta1 == null ? "" : respuesta1);
        request.setAttribute("respuesta2Ingresada", respuesta2 == null ? "" : respuesta2);

        boolean falta1 = respuesta1 == null || NormalizadorRespuestas.normalizar(respuesta1).isEmpty();
        boolean falta2 = respuesta2 == null || NormalizadorRespuestas.normalizar(respuesta2).isEmpty();
        if (falta1 || falta2) {
            recargarConMensaje(request, response, pendiente, "error.campos_respuestas");
            return;
        }

        if (!ValidadorEntrada.esRespuestaValida(respuesta1)
                || !ValidadorEntrada.esRespuestaValida(respuesta2)) {
            recargarConMensaje(request, response, pendiente, "error.formato_respuesta");
            return;
        }

        try {
            ResultadoAutenticacion resultado =
                    servicio.verificarRespuestas(pendiente, respuesta1, respuesta2);

            if (resultado.getTipo() == TipoResultado.RESPUESTAS_OK) {
                HttpSession sesion = request.getSession(true);
                sesion.setAttribute(ATRIBUTO_AUTENTICADO, Boolean.TRUE);
                sesion.setAttribute(ATRIBUTO_USUARIO_ID, resultado.getUsuarioId());
                sesion.removeAttribute(ATRIBUTO_PENDIENTE);
                request.changeSessionId();
                response.sendRedirect(request.getContextPath() + RUTA_ACCESO);
                return;
            }

            request.setAttribute("claveMensaje", resultado.getClaveMensaje());
            mostrarPreguntas(request, response, pendiente);
        } catch (CuentaIncompletaException excepcion) {
            mostrarErrorTecnico(request, response, excepcion, "CUENTA_INCOMPLETA");
        } catch (DataAccessException excepcion) {
            mostrarErrorTecnico(request, response, excepcion, "DATA_ACCESS_ERROR");
        }
    }

    private Long obtenerPendiente(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) {
            return null;
        }
        Object valor = sesion.getAttribute(ATRIBUTO_PENDIENTE);
        return valor instanceof Long ? (Long) valor : null;
    }

    private void recargarConMensaje(HttpServletRequest request, HttpServletResponse response,
                                    long usuarioId, String claveMensaje)
            throws ServletException, IOException {
        request.setAttribute("claveMensaje", claveMensaje);
        try {
            mostrarPreguntas(request, response, usuarioId);
        } catch (CuentaIncompletaException excepcion) {
            mostrarErrorTecnico(request, response, excepcion, "CUENTA_INCOMPLETA");
        } catch (DataAccessException excepcion) {
            mostrarErrorTecnico(request, response, excepcion, "DATA_ACCESS_ERROR");
        }
    }

    private void mostrarPreguntas(HttpServletRequest request, HttpServletResponse response, long usuarioId)
            throws ServletException, IOException {
        List<PreguntaSeguridad> preguntas = servicio.obtenerPreguntas(usuarioId);
        request.setAttribute("preguntas", preguntas);
        request.getRequestDispatcher(VISTA_PREGUNTAS).forward(request, response);
    }

    private void mostrarErrorTecnico(HttpServletRequest request, HttpServletResponse response,
                                     Exception excepcion, String evento) throws ServletException, IOException {
        String clase = excepcion.getClass().getName();
        REGISTRO.log(Level.SEVERE, evento + " clase=" + clase, excepcion);
        request.setAttribute("claveMensaje", "error.tecnico");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        request.getRequestDispatcher(VISTA_ERROR).forward(request, response);
    }
}
