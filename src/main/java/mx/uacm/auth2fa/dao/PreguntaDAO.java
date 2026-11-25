package mx.uacm.auth2fa.dao;

import java.util.List;

import mx.uacm.auth2fa.modelo.PreguntaSeguridad;

/**
 * Contrato de persistencia de las preguntas de seguridad.
 *
 * <p>La consulta de preguntas visibles no devuelve hashes. La consulta de
 * hashes es una operación separada que solo usa el servicio.</p>
 */
public interface PreguntaDAO {

    /**
     * Devuelve las preguntas de una cuenta, ordenadas por posición, sin hash.
     *
     * @param usuarioId identificador de la cuenta
     * @return entre cero y dos preguntas
     */
    List<PreguntaSeguridad> listarPorUsuario(long usuarioId);

    /**
     * Devuelve los hashes de las respuestas ordenados por posición.
     *
     * @param usuarioId identificador de la cuenta
     * @return hashes en el orden 1, 2
     */
    List<String> obtenerHashesPorUsuario(long usuarioId);
}
