package mx.uacm.auth2fa.dao;

import java.time.Instant;
import java.util.Optional;

import mx.uacm.auth2fa.modelo.Usuario;

/**
 * Contrato de persistencia de las cuentas.
 *
 * <p>Devuelve entidades del modelo y nunca filas JDBC, mensajes de interfaz ni
 * códigos HTTP. Los fallos técnicos se propagan como
 * {@link DataAccessException}.</p>
 */
public interface UsuarioDAO {

    /**
     * Recupera una cuenta por su nombre de acceso.
     *
     * @param nombreUsuario nombre con formato válido
     * @return la cuenta o {@link Optional#empty()} si no existe
     */
    Optional<Usuario> buscarPorNombre(String nombreUsuario);

    /**
     * Persiste el contador de intentos fallidos.
     *
     * @param id       identificador de la cuenta
     * @param intentos valor calculado, mayor o igual que cero
     */
    void actualizarIntentosFallidos(long id, int intentos);

    /**
     * Persiste la fecha de fin del bloqueo temporal.
     *
     * @param id     identificador de la cuenta
     * @param fecha  instante de desbloqueo
     */
    void guardarBloqueoHasta(long id, Instant fecha);

    /**
     * Limpia el contador y la fecha de bloqueo.
     *
     * @param id identificador de la cuenta
     */
    void reiniciarIntentos(long id);
}
