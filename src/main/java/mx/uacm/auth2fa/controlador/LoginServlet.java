package mx.uacm.auth2fa.controlador;

import java.io.IOException;
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
import mx.uacm.auth2fa.modelo.ResultadoAutenticacion;
import mx.uacm.auth2fa.modelo.ServicioAutenticacion;
import mx.uacm.auth2fa.modelo.TipoResultado;
import mx.uacm.auth2fa.util.ValidadorEntrada;

/**
 * Controlador de la primera etapa: usuario y contraseña.
 *
 * <p>Valida presencia y formato, delega en {@link ServicioAutenticacion} y
 * traduce el resultado en una vista o un avance a las preguntas. Nunca guarda
 * la contraseña en sesión, registro ni atributo de petición.</p>
 */
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger REGISTRO = Logger.getLogger(LoginServlet.class.getName());

    private static final String VISTA_LOGIN = "/WEB-INF/vistas/login.jsp";
    private static final String VISTA_ERROR = "/WEB-INF/vistas/error.jsp";
    private static final String RUTA_PREGUNTAS = "/preguntas";
    private static final String RUTA_ACCESO = "/acceso";

    private static final String ATRIBUTO_PENDIENTE = "usuarioIdPendiente";
    private static final String ATRIBUTO_NOMBRE = "nombreUsuario";
    private static final String ATRIBUTO_AUTENTICADO = "usuarioAutenticado";

    private transient ServicioAutenticacion servicio;

    @Override
    public void init() throws ServletException {
        servicio = new ServicioAutenticacion(new UsuarioDAOImpl(), new PreguntaDAOImpl());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession sesion = request.getSession(false);
        if (sesion != null && Boolean.TRUE.equals(sesion.getAttribute(ATRIBUTO_AUTENTICADO))) {
            response.sendRedirect(request.getContextPath() + RUTA_ACCESO);
            return;
        }

        String mensaje = null;
        if (request.getParameter("logout") != null) {
            mensaje = "aviso.logout";
        } else if ("sesion".equals(request.getParameter("error"))) {
            mensaje = "aviso.sesion";
        }
        request.setAttribute("claveMensaje", mensaje);
        request.getRequestDispatcher(VISTA_LOGIN).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String usuario = request.getParameter("usuario");
        String password = request.getParameter("password");
        request.setAttribute("usuarioIngresado", usuario == null ? "" : usuario);

        if (usuario == null || usuario.trim().isEmpty() || password == null || password.isEmpty()) {
            request.setAttribute("claveMensaje", "error.campos_login");
            request.getRequestDispatcher(VISTA_LOGIN).forward(request, response);
            return;
        }

        if (!ValidadorEntrada.esUsuarioValido(usuario)) {
            request.setAttribute("claveMensaje", "error.formato_usuario");
            request.getRequestDispatcher(VISTA_LOGIN).forward(request, response);
            return;
        }

        if (!ValidadorEntrada.esPasswordValida(password)) {
            request.setAttribute("claveMensaje", "error.formato_password");
            request.getRequestDispatcher(VISTA_LOGIN).forward(request, response);
            return;
        }

        try {
            ResultadoAutenticacion resultado = servicio.autenticarCredenciales(usuario, password);
            if (resultado.getTipo() == TipoResultado.CREDENCIALES_OK) {
                HttpSession sesion = request.getSession(true);
                sesion.setAttribute(ATRIBUTO_PENDIENTE, resultado.getUsuarioId());
                sesion.setAttribute(ATRIBUTO_NOMBRE, usuario);
                request.getRequestDispatcher(RUTA_PREGUNTAS).forward(request, response);
                return;
            }
            request.setAttribute("claveMensaje", resultado.getClaveMensaje());
            request.getRequestDispatcher(VISTA_LOGIN).forward(request, response);
        } catch (DataAccessException excepcion) {
            mostrarErrorTecnico(request, response, excepcion);
        }
    }

    private void mostrarErrorTecnico(HttpServletRequest request, HttpServletResponse response,
                                     Exception excepcion) throws ServletException, IOException {
        REGISTRO.log(Level.SEVERE, "DATA_ACCESS_ERROR clase=" + excepcion.getClass().getName(), excepcion);
        request.setAttribute("claveMensaje", "error.tecnico");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        request.getRequestDispatcher(VISTA_ERROR).forward(request, response);
    }
}
