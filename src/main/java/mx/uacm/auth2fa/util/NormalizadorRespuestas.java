package mx.uacm.auth2fa.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Aplica la normalización única de las respuestas de seguridad.
 *
 * <p>La misma función se usa al registrar y al verificar, de modo que las
 * mayúsculas, los acentos y los espacios adicionales no impiden un acceso
 * legítimo. La clase no tiene estado y nunca devuelve {@code null}.</p>
 */
public final class NormalizadorRespuestas {

    private NormalizadorRespuestas() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Normaliza una respuesta de seguridad.
     *
     * <p>Pasos, en orden: recortar extremos, colapsar espacios internos,
     * descomponer en NFD, eliminar diacríticos y convertir a minúsculas con
     * {@link Locale#ROOT}. La operación es idempotente.</p>
     *
     * @param valor respuesta original; no puede ser {@code null}
     * @return respuesta normalizada
     * @throws IllegalArgumentException si el valor es {@code null}
     */
    public static String normalizar(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("La respuesta a normalizar no puede ser nula");
        }
        String sinExtremos = valor.trim();
        String espaciosColapsados = sinExtremos.replaceAll("\\s+", " ");
        String descompuesto = Normalizer.normalize(espaciosColapsados, Normalizer.Form.NFD);
        String sinDiacriticos = descompuesto.replaceAll("\\p{M}+", "");
        return sinDiacriticos.toLowerCase(Locale.ROOT);
    }
}
