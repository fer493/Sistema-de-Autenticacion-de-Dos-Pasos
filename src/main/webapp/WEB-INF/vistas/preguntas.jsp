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
    <title>Preguntas de seguridad | Sistema 2FA</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/estilos.css">
</head>
<body>
<main class="pagina">
    <section class="tarjeta" aria-labelledby="titulo-preguntas">
        <header class="encabezado">
            <p class="institucion">Universidad Autónoma de la Ciudad de México</p>
            <h1 id="titulo-preguntas">Verificación de seguridad</h1>
            <p class="subtitulo">Segundo paso de la autenticación</p>
        </header>

        <form action="${pageContext.request.contextPath}/preguntas" method="post" novalidate>
            <div class="campo">
                <label id="pregunta1" for="respuesta1"><c:out value="${preguntas[0].texto}"/></label>
                <input type="text"
                       id="respuesta1"
                       name="respuesta1"
                       value="${fn:escapeXml(respuesta1Ingresada)}"
                       maxlength="100"
                       autocomplete="off"
                       autofocus>
            </div>

            <div class="campo">
                <label id="pregunta2" for="respuesta2"><c:out value="${preguntas[1].texto}"/></label>
                <input type="text"
                       id="respuesta2"
                       name="respuesta2"
                       value="${fn:escapeXml(respuesta2Ingresada)}"
                       maxlength="100"
                       autocomplete="off">
            </div>

            <p id="mensaje" class="mensaje" role="alert" aria-live="polite"><c:if test="${not empty claveMensaje}"><fmt:message key="${claveMensaje}"/></c:if></p>

            <button type="submit" id="btnVerificar" name="btnVerificar">Verificar</button>
        </form>

        <p class="pie">Proyecto académico · Aseguramiento y Calidad del Software</p>
    </section>
</main>
</body>
</html>
