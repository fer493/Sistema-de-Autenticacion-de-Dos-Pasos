package mx.uacm.auth2fa.controlador;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Controlador de cierre de sesión.
 *
 * <p>No delega en el servicio porque {@code HttpSession} es una
 * responsabilidad web. La operación es idempotente: si no existe sesión, solo
 * redirige al acceso con el aviso correspondiente.</p>
 */
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger REGISTRO = Logger.getLogger(LogoutServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            Object usuarioId = sesion.getAttribute("usuarioId");
            sesion.invalidate();
            REGISTRO.info(() -> "LOGOUT usuarioId=" + usuarioId);
        }
        response.sendRedirect(request.getContextPath() + "/login?logout=1");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}
