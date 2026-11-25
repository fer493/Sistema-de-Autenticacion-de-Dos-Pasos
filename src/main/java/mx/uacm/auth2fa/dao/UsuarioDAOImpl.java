package mx.uacm.auth2fa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import mx.uacm.auth2fa.modelo.EstadoUsuario;
import mx.uacm.auth2fa.modelo.Usuario;

/**
 * Implementación JDBC de {@link UsuarioDAO}.
 *
 * <p>Todas las consultas usan {@link PreparedStatement}. Cada operación abre y
 * cierra su propia conexión con try-with-resources. No se registran
 * contraseñas ni hashes.</p>
 */
public class UsuarioDAOImpl implements UsuarioDAO {

    private static final String SQL_BUSCAR =
            "SELECT id, nombre_usuario, hash_password, estado, intentos_fallidos, bloqueado_hasta "
            + "FROM usuarios WHERE nombre_usuario = ?";
    private static final String SQL_ACTUALIZAR_INTENTOS =
            "UPDATE usuarios SET intentos_fallidos = ? WHERE id = ?";
    private static final String SQL_GUARDAR_BLOQUEO =
            "UPDATE usuarios SET bloqueado_hasta = ? WHERE id = ?";
    private static final String SQL_REINICIAR =
            "UPDATE usuarios SET intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id = ?";

    @Override
    public Optional<Usuario> buscarPorNombre(String nombreUsuario) {
        if (nombreUsuario == null || nombreUsuario.isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        try (Connection conexion = ConexionBD.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR)) {
            sentencia.setString(1, nombreUsuario);
            try (ResultSet fila = sentencia.executeQuery()) {
                if (!fila.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapear(fila));
            }
        } catch (SQLException excepcion) {
            throw new DataAccessException("No fue posible buscar la cuenta", excepcion);
        }
    }

    @Override
    public void actualizarIntentosFallidos(long id, int intentos) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador de la cuenta debe ser mayor que cero");
        }
        if (intentos < 0) {
            throw new IllegalArgumentException("Los intentos fallidos no pueden ser negativos");
        }
        ejecutarActualizacion(SQL_ACTUALIZAR_INTENTOS, sentencia -> {
            sentencia.setInt(1, intentos);
            sentencia.setLong(2, id);
        });
    }

    @Override
    public void guardarBloqueoHasta(long id, Instant fecha) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador de la cuenta debe ser mayor que cero");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de desbloqueo es obligatoria");
        }
        ejecutarActualizacion(SQL_GUARDAR_BLOQUEO, sentencia -> {
            sentencia.setTimestamp(1, Timestamp.from(fecha));
            sentencia.setLong(2, id);
        });
    }

    @Override
    public void reiniciarIntentos(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador de la cuenta debe ser mayor que cero");
        }
        ejecutarActualizacion(SQL_REINICIAR, sentencia -> sentencia.setLong(1, id));
    }

    private void ejecutarActualizacion(String sql, Asignador asignador) {
        try (Connection conexion = ConexionBD.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            asignador.asignar(sentencia);
            sentencia.executeUpdate();
        } catch (SQLException excepcion) {
            throw new DataAccessException("No fue posible actualizar la cuenta", excepcion);
        }
    }

    private static Usuario mapear(ResultSet fila) throws SQLException {
        long id = fila.getLong("id");
        String nombre = fila.getString("nombre_usuario");
        String hash = fila.getString("hash_password");
        EstadoUsuario estado = EstadoUsuario.valueOf(fila.getString("estado"));
        int intentos = fila.getInt("intentos_fallidos");
        Timestamp bloqueo = fila.getTimestamp("bloqueado_hasta");
        Instant bloqueadoHasta = bloqueo == null ? null : bloqueo.toInstant();
        return new Usuario(id, nombre, hash, estado, intentos, bloqueadoHasta);
    }

    @FunctionalInterface
    private interface Asignador {
        void asignar(PreparedStatement sentencia) throws SQLException;
    }
}
