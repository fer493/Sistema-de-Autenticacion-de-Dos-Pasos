package mx.uacm.auth2fa.util;

import java.util.regex.Pattern;

/**
 * Validaciones de formato previas a la lógica de negocio.
 *
 * <p>La clase no tiene estado. No recorta ni transforma los valores: un
 * espacio al inicio o al final del usuario es un error de formato y un espacio
 * dentro de la contraseña forma parte del secreto.</p>
 */
public final class ValidadorEntrada {

    private static final Pattern USUARIO = Pattern.compile("^[A-Za-z0-9._-]{4,20}$");

    private ValidadorEntrada() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Comprueba el formato del nombre de usuario.
     *
     * <p>Debe tener de 4 a 20 caracteres y coincidir con
     * {@code ^[A-Za-z0-9._-]{4,20}$}. No se recorta el valor.</p>
     *
     * @param usuario nombre escrito por la persona
     * @return {@code true} solo si cumple el formato
     */
    public static boolean esUsuarioValido(String usuario) {
        return usuario != null && USUARIO.matcher(usuario).matches();
    }

    /**
     * Comprueba la longitud y la complejidad de una contraseña.
     *
     * <p>Debe tener de 8 a 64 caracteres, al menos una mayúscula, una
     * minúscula, un dígito y un carácter especial imprimible. Un espacio no
     * cuenta como carácter especial.</p>
     *
     * @param password contraseña escrita por la persona
     * @return {@code true} solo si cumple la política
     */
    public static boolean esPasswordValida(String password) {
        if (password == null) {
            return false;
        }
        int longitud = password.codePointCount(0, password.length());
        if (longitud < 8 || longitud > 64) {
            return false;
        }
        boolean mayuscula = false;
        boolean minuscula = false;
        boolean digito = false;
        boolean especial = false;
        for (int i = 0; i < password.length(); ) {
            int codigo = password.codePointAt(i);
            if (Character.isUpperCase(codigo)) {
                mayuscula = true;
            } else if (Character.isLowerCase(codigo)) {
                minuscula = true;
            } else if (Character.isDigit(codigo)) {
                digito = true;
            } else if (!Character.isWhitespace(codigo) && !Character.isISOControl(codigo)) {
                especial = true;
            }
            i += Character.charCount(codigo);
        }
        return mayuscula && minuscula && digito && especial;
    }

    /**
     * Comprueba la longitud de una respuesta después de normalizarla.
     *
     * <p>El texto normalizado debe tener entre 3 y 100 caracteres y no puede
     * quedar vacío. Se rechazan caracteres de control.</p>
     *
     * @param respuesta respuesta escrita por la persona
     * @return {@code true} solo si la respuesta es aceptable
     */
    public static boolean esRespuestaValida(String respuesta) {
        if (respuesta == null || contieneControl(respuesta)) {
            return false;
        }
        String normalizada = NormalizadorRespuestas.normalizar(respuesta);
        int longitud = normalizada.codePointCount(0, normalizada.length());
        return longitud >= 3 && longitud <= 100;
    }

    private static boolean contieneControl(String valor) {
        for (int i = 0; i < valor.length(); ) {
            int codigo = valor.codePointAt(i);
            if (Character.isISOControl(codigo)) {
                return true;
            }
            i += Character.charCount(codigo);
        }
        return false;
    }
}
