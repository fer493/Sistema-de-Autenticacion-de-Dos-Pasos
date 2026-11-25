package mx.uacm.auth2fa.modelo;

/**
 * Señala que una cuenta no tiene exactamente dos preguntas con orden 1 y 2.
 *
 * <p>Es un error de configuración, no un fallo del usuario. El controlador la
 * captura, registra el identificador de la cuenta y muestra el mensaje
 * técnico genérico sin conceder acceso.</p>
 */
public class CuentaIncompletaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CuentaIncompletaException(String mensaje) {
        super(mensaje);
    }

    public CuentaIncompletaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
