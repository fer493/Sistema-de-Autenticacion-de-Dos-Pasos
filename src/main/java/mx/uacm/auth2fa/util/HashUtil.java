package mx.uacm.auth2fa.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Genera y verifica hashes PBKDF2 de contraseñas y respuestas.
 *
 * <p>Formato almacenado: {@code pbkdf2$210000$salB64$hashB64}. Cada llamada usa
 * una sal aleatoria de 16 bytes y deriva 32 bytes con
 * {@code PBKDF2WithHmacSHA256}. La comparación se hace con
 * {@link MessageDigest#isEqual(byte[], byte[])} para reducir el riesgo de
 * ataques de tiempo. La clase nunca registra el secreto.</p>
 */
public final class HashUtil {

    private static final String PREFIJO = "pbkdf2";
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 210_000;
    private static final int LONGITUD_SAL = 16;
    private static final int LONGITUD_CLAVE = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private HashUtil() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Calcula el hash de una contraseña.
     *
     * @param password contraseña no nula y no vacía; no se normaliza
     * @return hash con sal e iteraciones en el formato documentado
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        return hashear(password);
    }

    /**
     * Verifica una contraseña contra un hash almacenado.
     *
     * @param passwordCandidata contraseña escrita por la persona
     * @param hashAlmacenado    hash en el formato documentado
     * @return {@code true} solo si coincide; {@code false} ante un hash inválido
     */
    public static boolean verificarPassword(String passwordCandidata, String hashAlmacenado) {
        if (passwordCandidata == null || passwordCandidata.isEmpty() || hashAlmacenado == null) {
            return false;
        }
        return verificar(passwordCandidata, hashAlmacenado);
    }

    /**
     * Calcula el hash de una respuesta ya normalizada.
     *
     * @param respuestaNormalizada respuesta normalizada
     * @return hash con sal e iteraciones en el formato documentado
     */
    public static String hashRespuesta(String respuestaNormalizada) {
        if (respuestaNormalizada == null || respuestaNormalizada.isEmpty()) {
            throw new IllegalArgumentException("La respuesta no puede estar vacía");
        }
        return hashear(respuestaNormalizada);
    }

    /**
     * Verifica una respuesta normalizada contra un hash almacenado.
     *
     * @param respuestaNormalizada respuesta ya normalizada
     * @param hashAlmacenado       hash en el formato documentado
     * @return {@code true} solo si coincide; {@code false} ante un hash inválido
     */
    public static boolean verificarRespuesta(String respuestaNormalizada, String hashAlmacenado) {
        if (respuestaNormalizada == null || respuestaNormalizada.isEmpty() || hashAlmacenado == null) {
            return false;
        }
        return verificar(respuestaNormalizada, hashAlmacenado);
    }

    private static String hashear(String valor) {
        byte[] sal = new byte[LONGITUD_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] derivada = derivar(valor, sal, ITERACIONES);
        return PREFIJO + '$' + ITERACIONES + '$'
                + Base64.getEncoder().encodeToString(sal) + '$'
                + Base64.getEncoder().encodeToString(derivada);
    }

    private static boolean verificar(String valor, String hashAlmacenado) {
        String[] partes = hashAlmacenado.split("\\$", -1);
        if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            if (iteraciones <= 0) {
                return false;
            }
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            if (sal.length == 0 || esperado.length == 0) {
                return false;
            }
            byte[] calculado = derivar(valor, sal, iteraciones, esperado.length);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException excepcion) {
            // Formato inválido (Base64 o iteraciones): se trata como no coincidente.
            return false;
        }
    }

    private static byte[] derivar(String valor, byte[] sal, int iteraciones) {
        return derivar(valor, sal, iteraciones, LONGITUD_CLAVE);
    }

    private static byte[] derivar(String valor, byte[] sal, int iteraciones, int longitud) {
        PBEKeySpec especificacion = new PBEKeySpec(valor.toCharArray(), sal, iteraciones, longitud * 8);
        try {
            SecretKeyFactory fabrica = SecretKeyFactory.getInstance(ALGORITMO);
            return fabrica.generateSecret(especificacion).getEncoded();
        } catch (GeneralSecurityException excepcion) {
            throw new IllegalStateException("No fue posible calcular el hash PBKDF2", excepcion);
        } finally {
            especificacion.clearPassword();
        }
    }
}
