package mx.uacm.auth2fa.modelo;

import java.time.Instant;

/**
 * Cuenta del sistema 2FA.
 *
 * <p>La entidad no depende de la web ni de JDBC. El hash de la contraseña se
 * transporta porque el servicio lo necesita para comparar; nunca se expone a
 * una vista ni se escribe en el registro.</p>
 */
public final class Usuario {

    private final long id;
    private final String nombreUsuario;
    private final String hashPassword;
    private final EstadoUsuario estado;
    private final int intentosFallidos;
    private final Instant bloqueadoHasta;

    public Usuario(long id,
                   String nombreUsuario,
                   String hashPassword,
                   EstadoUsuario estado,
                   int intentosFallidos,
                   Instant bloqueadoHasta) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador del usuario debe ser mayor que cero");
        }
        if (nombreUsuario == null || nombreUsuario.isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        if (hashPassword == null || hashPassword.isEmpty()) {
            throw new IllegalArgumentException("El hash de la contraseña es obligatorio");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado del usuario es obligatorio");
        }
        if (intentosFallidos < 0) {
            throw new IllegalArgumentException("Los intentos fallidos no pueden ser negativos");
        }
        this.id = id;
        this.nombreUsuario = nombreUsuario;
        this.hashPassword = hashPassword;
        this.estado = estado;
        this.intentosFallidos = intentosFallidos;
        this.bloqueadoHasta = bloqueadoHasta;
    }

    /**
     * Evalúa el bloqueo efectivo de la cuenta en un instante dado.
     *
     * <p>Un estado administrativo {@code BLOQUEADO} prevalece sobre la fecha.
     * Si el estado es {@code ACTIVO}, solo bloquea una fecha de desbloqueo
     * posterior al instante recibido; una fecha vencida ya no bloquea.</p>
     *
     * @param ahora instante de referencia
     * @return {@code true} si la cuenta no debe permitir el acceso
     */
    public boolean estaBloqueado(Instant ahora) {
        if (ahora == null) {
            throw new IllegalArgumentException("El instante de referencia es obligatorio");
        }
        if (estado == EstadoUsuario.BLOQUEADO) {
            return true;
        }
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    /**
     * Indica si la fecha de desbloqueo existe y ya venció.
     *
     * @param ahora instante de referencia
     * @return {@code true} si hay una fecha anterior o igual al instante actual
     */
    public boolean tieneBloqueoVencido(Instant ahora) {
        if (ahora == null) {
            throw new IllegalArgumentException("El instante de referencia es obligatorio");
        }
        return bloqueadoHasta != null && !bloqueadoHasta.isAfter(ahora);
    }

    public long getId() {
        return id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getHashPassword() {
        return hashPassword;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    @Override
    public String toString() {
        // No se incluye el hash para evitar filtrar material sensible en trazas.
        return "Usuario{id=" + id
                + ", nombreUsuario='" + nombreUsuario + '\''
                + ", estado=" + estado
                + ", intentosFallidos=" + intentosFallidos
                + ", bloqueadoHasta=" + bloqueadoHasta
                + '}';
    }
}
