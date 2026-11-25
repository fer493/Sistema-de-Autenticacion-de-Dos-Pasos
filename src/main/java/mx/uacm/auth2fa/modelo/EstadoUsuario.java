package mx.uacm.auth2fa.modelo;

/**
 * Estado lógico de una cuenta.
 *
 * <p>El estado {@link #BLOQUEADO} representa una decisión administrativa y
 * impide el acceso aunque no exista una fecha de desbloqueo. El bloqueo
 * temporal por intentos fallidos se deduce de {@code bloqueadoHasta} y no
 * cambia este valor.</p>
 */
public enum EstadoUsuario {
    ACTIVO,
    BLOQUEADO
}
