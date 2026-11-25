package mx.uacm.auth2fa.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Punto único de obtención de conexiones JDBC.
 *
 * <p>Lee {@code db.properties} del classpath, carga el controlador de MySQL y
 * entrega una conexión nueva por operación. No mantiene conexiones abiertas ni
 * expone las credenciales.</p>
 */
public final class ConexionBD {

    private static final String ARCHIVO = "/db.properties";
    private static final String URL;
    private static final String USUARIO;
    private static final String PASSWORD;
    private static final String ERROR_CONFIGURACION;

    static {
        Properties propiedades = new Properties();
        String error = null;
        try (InputStream entrada = ConexionBD.class.getResourceAsStream(ARCHIVO)) {
            if (entrada == null) {
                error = "No se encontró " + ARCHIVO + " en el classpath";
            } else {
                try (Reader lector = new InputStreamReader(entrada, StandardCharsets.UTF_8)) {
                    propiedades.load(lector);
                }
                Class.forName(propiedades.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
            }
        } catch (IOException | ClassNotFoundException excepcion) {
            error = "No fue posible cargar la configuración de la base de datos";
        }
        URL = propiedades.getProperty("db.url");
        USUARIO = propiedades.getProperty("db.usuario");
        PASSWORD = propiedades.getProperty("db.password");
        if (error == null && (URL == null || USUARIO == null || PASSWORD == null)) {
            error = "La configuración de la base de datos está incompleta";
        }
        ERROR_CONFIGURACION = error;
    }

    private ConexionBD() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Abre una conexión configurada.
     *
     * @return conexión nueva; el llamador debe cerrarla con try-with-resources
     * @throws DataAccessException si falta la configuración o falla la conexión
     */
    public static Connection getConnection() {
        if (ERROR_CONFIGURACION != null) {
            throw new DataAccessException(ERROR_CONFIGURACION);
        }
        try {
            return DriverManager.getConnection(URL, USUARIO, PASSWORD);
        } catch (SQLException excepcion) {
            throw new DataAccessException("No fue posible establecer la conexión con la base de datos", excepcion);
        }
    }
}
