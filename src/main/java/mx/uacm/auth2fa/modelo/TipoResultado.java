package mx.uacm.auth2fa.modelo;

/**
 * Tipos de resultado que puede producir el servicio de autenticación.
 *
 * <p>No se agregan estados intermedios: un fallo de credenciales y un fallo de
 * respuestas son resultados distintos, y un error técnico se modela con una
 * excepción, no con un valor de este enumerado.</p>
 */
public enum TipoResultado {
    CREDENCIALES_OK,
    CREDENCIALES_INVALIDAS,
    CUENTA_BLOQUEADA,
    RESPUESTAS_OK,
    RESPUESTAS_INVALIDAS;

    /**
     * Indica si el resultado representa el final exitoso de una etapa.
     *
     * @return {@code true} para {@code CREDENCIALES_OK} y {@code RESPUESTAS_OK}
     */
    public boolean esExitoso() {
        return this == CREDENCIALES_OK || this == RESPUESTAS_OK;
    }
}
