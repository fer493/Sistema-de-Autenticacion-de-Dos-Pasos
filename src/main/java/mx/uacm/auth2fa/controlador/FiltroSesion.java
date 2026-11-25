package mx.uacm.auth2fa.controlador;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Protege la vista de acceso autenticado.
 *
 * <p>Si no existe sesión o {@code usuarioAutenticado} no es verdadero, no
 * sirve el recurso y redirige al login con el aviso de sesión requerida. No
 * crea sesiones ni modifica atributos.</p>
 */
public class FiltroSesion implements Filter {

    private static final Logger REGISTRO = Logger.getLogger(FiltroSesion.class.getName());

    @Override
    public void init(FilterConfig configuracion) {
        // Sin parámetros de configuración.
    }

    @Override
    public void doFilter(ServletRequest peticion, ServletResponse respuesta, FilterChain cadena)
            throws IOException, ServletException {
        HttpServletRequest solicitud = (HttpServletRequest) peticion;
        HttpServletResponse salida = (HttpServletResponse) respuesta;
        HttpSession sesion = solicitud.getSession(false);

        if (sesion == null || !Boolean.TRUE.equals(sesion.getAttribute("usuarioAutenticado"))) {
            REGISTRO.info(() -> "ACCESO_DENEGADO uri=" + solicitud.getRequestURI()
                    + " conSesion=" + (sesion != null));
            salida.sendRedirect(solicitud.getContextPath() + "/login?error=sesion");
            return;
        }

        salida.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        salida.setHeader("Pragma", "no-cache");
        salida.setDateHeader("Expires", 0);
        cadena.doFilter(peticion, respuesta);
    }

    @Override
    public void destroy() {
        // Sin recursos que liberar.
    }
}
