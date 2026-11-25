<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Acceso autorizado | Sistema 2FA</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/estilos.css">
</head>
<body>
<main class="pagina">
    <section class="tarjeta" id="contenidoProtegido" aria-labelledby="titulo-acceso-autorizado">
        <header class="encabezado">
            <p class="institucion">Universidad Autónoma de la Ciudad de México</p>
            <h1 id="titulo-acceso-autorizado">Acceso autorizado</h1>
            <p class="subtitulo">Autenticación de dos pasos completada</p>
        </header>

        <p id="saludo" class="saludo">Bienvenido, <c:out value="${sessionScope.nombreUsuario}"/></p>

        <p class="texto-protegido">Ha completado la verificación de credenciales y de preguntas de seguridad. Esta sección solo está disponible con una sesión autenticada.</p>

        <form action="${pageContext.request.contextPath}/logout" method="post">
            <button type="submit" id="btnSalir" name="btnSalir">Cerrar sesión</button>
        </form>

        <p class="pie">Proyecto académico · Aseguramiento y Calidad del Software</p>
    </section>
</main>
</body>
</html>
