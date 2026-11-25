-- Sistema 2FA - esquema de base de datos para MySQL 8.0
-- Basado en el diccionario de datos y las restricciones de la Entrega 3.

CREATE DATABASE IF NOT EXISTS auth2fa
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE auth2fa;

SET NAMES utf8mb4;

DROP TABLE IF EXISTS preguntas_seguridad;
DROP TABLE IF EXISTS usuarios;

CREATE TABLE usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre_usuario VARCHAR(20) NOT NULL,
    hash_password VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    intentos_fallidos INT NOT NULL DEFAULT 0,
    bloqueado_hasta DATETIME NULL,
    fecha_creacion DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_usuarios_nombre_usuario (nombre_usuario),
    CONSTRAINT chk_usuarios_estado CHECK (estado IN ('ACTIVO', 'BLOQUEADO')),
    CONSTRAINT chk_usuarios_intentos CHECK (intentos_fallidos >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE preguntas_seguridad (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    orden TINYINT NOT NULL,
    texto VARCHAR(120) NOT NULL,
    hash_respuesta VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_preguntas_usuario_orden (usuario_id, orden),
    KEY idx_preguntas_usuario (usuario_id),
    CONSTRAINT fk_preguntas_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_preguntas_orden CHECK (orden IN (1, 2))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
