package mx.uacm.auth2fa.modelo;

/**
 * Pregunta de seguridad asociada a una cuenta.
 *
 * <p>La vista recibe instancias sin hash. El servicio obtiene los hashes por
 * una operación separada del DAO para que un descuido de la JSP no los
 * exponga.</p>
 */
public final class PreguntaSeguridad {

    private final long id;
    private final long usuarioId;
    private final int orden;
    private final String texto;
    private final String hashRespuesta;

    public PreguntaSeguridad(long id, long usuarioId, int orden, String texto, String hashRespuesta) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador de la pregunta debe ser mayor que cero");
        }
        if (usuarioId <= 0) {
            throw new IllegalArgumentException("El identificador del usuario debe ser mayor que cero");
        }
        if (orden != 1 && orden != 2) {
            throw new IllegalArgumentException("El orden de la pregunta debe ser 1 o 2");
        }
        if (texto == null || texto.length() < 10 || texto.length() > 120) {
            throw new IllegalArgumentException("El texto de la pregunta debe tener entre 10 y 120 caracteres");
        }
        this.id = id;
        this.usuarioId = usuarioId;
        this.orden = orden;
        this.texto = texto;
        this.hashRespuesta = hashRespuesta;
    }

    /**
     * Devuelve una copia de la pregunta sin el hash de la respuesta.
     *
     * @return pregunta apta para la vista
     */
    public PreguntaSeguridad sinHash() {
        return new PreguntaSeguridad(id, usuarioId, orden, texto, null);
    }

    public long getId() {
        return id;
    }

    public long getUsuarioId() {
        return usuarioId;
    }

    public int getOrden() {
        return orden;
    }

    public String getTexto() {
        return texto;
    }

    public String getHashRespuesta() {
        return hashRespuesta;
    }

    @Override
    public String toString() {
        return "PreguntaSeguridad{id=" + id
                + ", usuarioId=" + usuarioId
                + ", orden=" + orden
                + ", texto='" + texto + '\''
                + '}';
    }
}
