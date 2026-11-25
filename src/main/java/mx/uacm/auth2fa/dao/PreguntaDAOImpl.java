package mx.uacm.auth2fa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import mx.uacm.auth2fa.modelo.PreguntaSeguridad;

/**
 * Implementación JDBC de {@link PreguntaDAO}.
 *
 * <p>La lista expuesta a la vista se construye con el hash en {@code null}.
 * Los hashes solo se entregan por {@link #obtenerHashesPorUsuario(long)}.</p>
 */
public class PreguntaDAOImpl implements PreguntaDAO {

    private static final String SQL_LISTAR =
            "SELECT id, usuario_id, orden, texto, hash_respuesta "
            + "FROM preguntas_seguridad WHERE usuario_id = ? ORDER BY orden";
    private static final String SQL_HASHES =
            "SELECT hash_respuesta FROM preguntas_seguridad "
            + "WHERE usuario_id = ? ORDER BY orden";

    @Override
    public List<PreguntaSeguridad> listarPorUsuario(long usuarioId) {
        validarUsuario(usuarioId);
        List<PreguntaSeguridad> preguntas = new ArrayList<>();
        try (Connection conexion = ConexionBD.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR)) {
            sentencia.setLong(1, usuarioId);
            try (ResultSet fila = sentencia.executeQuery()) {
                while (fila.next()) {
                    PreguntaSeguridad pregunta = new PreguntaSeguridad(
                            fila.getLong("id"),
                            fila.getLong("usuario_id"),
                            fila.getInt("orden"),
                            fila.getString("texto"),
                            null);
                    preguntas.add(pregunta);
                }
            }
        } catch (SQLException excepcion) {
            throw new DataAccessException("No fue posible consultar las preguntas de seguridad", excepcion);
        }
        return preguntas;
    }

    @Override
    public List<String> obtenerHashesPorUsuario(long usuarioId) {
        validarUsuario(usuarioId);
        List<String> hashes = new ArrayList<>();
        try (Connection conexion = ConexionBD.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_HASHES)) {
            sentencia.setLong(1, usuarioId);
            try (ResultSet fila = sentencia.executeQuery()) {
                while (fila.next()) {
                    hashes.add(fila.getString("hash_respuesta"));
                }
            }
        } catch (SQLException excepcion) {
            throw new DataAccessException("No fue posible consultar los hashes de las respuestas", excepcion);
        }
        return hashes;
    }

    private static void validarUsuario(long usuarioId) {
        if (usuarioId <= 0) {
            throw new IllegalArgumentException("El identificador de la cuenta debe ser mayor que cero");
        }
    }
}
