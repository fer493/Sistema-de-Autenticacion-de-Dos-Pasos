package mx.uacm.auth2fa.modelo;

/**
 * Resultado de una operación de autenticación.
 *
 * <p>Transporta el tipo, la clave de mensaje del catálogo y el identificador
 * del usuario. No contiene contraseñas, respuestas ni hashes. Un resultado
 * exitoso siempre lleva un identificador mayor que cero; un fallo usa cero.</p>
 */
public final class ResultadoAutenticacion {

    private final TipoResultado tipo;
    private final String claveMensaje;
    private final long usuarioId;

    public ResultadoAutenticacion(TipoResultado tipo, String claveMensaje, long usuarioId) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de resultado es obligatorio");
        }
        if (tipo.esExitoso() && usuarioId <= 0) {
            throw new IllegalArgumentException("Un resultado exitoso requiere un usuario válido");
        }
        if (!tipo.esExitoso() && usuarioId != 0) {
            throw new IllegalArgumentException("Un resultado fallido no transporta usuario");
        }
        this.tipo = tipo;
        this.claveMensaje = claveMensaje;
        this.usuarioId = usuarioId;
    }

    public static ResultadoAutenticacion exitoso(TipoResultado tipo, long usuarioId) {
        return new ResultadoAutenticacion(tipo, null, usuarioId);
    }

    public static ResultadoAutenticacion fallido(TipoResultado tipo, String claveMensaje) {
        return new ResultadoAutenticacion(tipo, claveMensaje, 0L);
    }

    public TipoResultado getTipo() {
        return tipo;
    }

    public String getClaveMensaje() {
        return claveMensaje;
    }

    public long getUsuarioId() {
        return usuarioId;
    }

    public boolean esExitoso() {
        return tipo.esExitoso();
    }

    @Override
    public String toString() {
        return "ResultadoAutenticacion{tipo=" + tipo
                + ", claveMensaje='" + claveMensaje + '\''
                + ", usuarioId=" + usuarioId
                + '}';
    }
}
