<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<fmt:setBundle basename="mensajes"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Acceso | Sistema 2FA</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/estilos.css">
</head>
<body>
<main class="pagina">
    <section class="tarjeta" aria-labelledby="titulo-acceso">
        <header class="encabezado">
            <p class="institucion">Universidad Autónoma de la Ciudad de México</p>
            <h1 id="titulo-acceso">Acceso al sistema</h1>
            <p class="subtitulo">Autenticación de dos pasos</p>
        </header>

        <form action="${pageContext.request.contextPath}/login" method="post" novalidate>
            <div class="campo">
                <label for="usuario">Usuario</label>
                <input type="text"
                       id="usuario"
                       name="usuario"
                       value="${fn:escapeXml(usuarioIngresado)}"
                       maxlength="20"
                       autocomplete="username"
                       autofocus>
            </div>

            <div class="campo">
                <label for="password">Contraseña</label>
                <input type="password"
                       id="password"
                       name="password"
                       maxlength="64"
                       autocomplete="current-password">
            </div>

            <p id="mensaje" class="mensaje" role="alert" aria-live="polite"><c:if test="${not empty claveMensaje}"><fmt:message key="${claveMensaje}"/></c:if></p>

            <button type="submit" id="btnIngresar" name="btnIngresar">Ingresar</button>
        </form>

        <p class="pie">Proyecto académico · Aseguramiento y Calidad del Software</p>
    </section>
</main>
</body>
</html>
