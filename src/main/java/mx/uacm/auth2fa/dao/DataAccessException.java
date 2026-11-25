package mx.uacm.auth2fa.dao;

/**
 * Excepción controlada para fallos de persistencia.
 *
 * <p>Envuelve cualquier {@code SQLException} para que el paquete DAO sea el
 * único que conoce JDBC. El controlador la captura y muestra el mensaje
 * técnico genérico; el detalle se registra con {@code java.util.logging}.</p>
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String mensaje) {
        super(mensaje);
    }

    public DataAccessException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
