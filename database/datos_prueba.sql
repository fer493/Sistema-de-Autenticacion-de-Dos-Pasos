-- Sistema 2FA - datos de prueba para el entorno de desarrollo
-- Entrega 3. No usar en produccion.
-- Requiere haber ejecutado esquema.sql.
-- Las respuestas se guardan normalizadas (minusculas, sin acentos, espacios colapsados).

USE auth2fa;

DELETE FROM preguntas_seguridad;
DELETE FROM usuarios;
ALTER TABLE usuarios AUTO_INCREMENT = 1;
ALTER TABLE preguntas_seguridad AUTO_INCREMENT = 1;
-- TD-01, TD-05, TD-06
INSERT INTO usuarios (id, nombre_usuario, hash_password, estado, intentos_fallidos, bloqueado_hasta, fecha_creacion) VALUES (1, 'usuario.prueba', 'pbkdf2$210000$sdyQOQCxzoRbXFwOkxmtjQ==$gxyFlj0RTqJZImm/utBJuDL13aGusaMKr22k8GHWhUM=', 'ACTIVO', 0, NULL, '2026-11-01 09:00:00');
-- TD-02
INSERT INTO usuarios (id, nombre_usuario, hash_password, estado, intentos_fallidos, bloqueado_hasta, fecha_creacion) VALUES (2, 'ana.lopez', 'pbkdf2$210000$ul0RekhWTNXaEfZp7Nfewg==$Z0INSPax2CugfeM5D6F5aAdTXSFB8dh0O6Y7EBu8Qi8=', 'ACTIVO', 0, NULL, '2026-11-01 09:00:00');
-- bloqueo vigente
INSERT INTO usuarios (id, nombre_usuario, hash_password, estado, intentos_fallidos, bloqueado_hasta, fecha_creacion) VALUES (3, 'cuenta.bloqueada', 'pbkdf2$210000$SOeO+nrkzhtNdmmsrpqBsA==$SN29VHpKSXTiXWGtY0CDWenfD5WXwvGBWLGWOkiRzxg=', 'ACTIVO', 3, DATE_ADD(NOW(), INTERVAL 15 MINUTE), '2026-11-01 09:00:00');
-- bloqueo vencido
INSERT INTO usuarios (id, nombre_usuario, hash_password, estado, intentos_fallidos, bloqueado_hasta, fecha_creacion) VALUES (4, 'cuenta.vencida', 'pbkdf2$210000$5e6mCc7OxSBWRcyTCSgKaA==$pvTif2rL1cSmAWtOuaD+9+2orIS37r4+/8JjASANFPg=', 'ACTIVO', 3, DATE_SUB(NOW(), INTERVAL 1 MINUTE), '2026-11-01 09:00:00');
-- TD-09, usuario de 4
INSERT INTO usuarios (id, nombre_usuario, hash_password, estado, intentos_fallidos, bloqueado_hasta, fecha_creacion) VALUES (5, 'usua', 'pbkdf2$210000$KUhzEflHm8LtzPfxOHVODA==$JuiEOI0RX7XSHwMfAIQATBUieo1r0laLiH6VHxE9hDw=', 'ACTIVO', 0, NULL, '2026-11-01 09:00:00');
-- TD-11, usuario de 20
INSERT INTO usuarios (id, nombre_usuario, hash_password, estado, intentos_fallidos, bloqueado_hasta, fecha_creacion) VALUES (6, 'usuario.veinti.chars', 'pbkdf2$210000$/bst/K4+iPb0LMIUmdQ56w==$3LJoFFmvJulknMi200phA+LC6zbNyADQpEJADIelzLc=', 'ACTIVO', 0, NULL, '2026-11-01 09:00:00');
INSERT INTO preguntas_seguridad (id, usuario_id, orden, texto, hash_respuesta) VALUES (1, 1, 1, '¿Nombre de tu primera mascota?', 'pbkdf2$210000$ZbcZsqccOET1qjPqn32QdQ==$lX4Hijc4jbALNA7cc3QmAxoULFaItv2NWWFC7skW3g4=');
INSERT INTO preguntas_seguridad (id, usuario_id, orden, texto, hash_respuesta) VALUES (2, 1, 2, '¿Ciudad donde naciste?', 'pbkdf2$210000$gR6GWjUy7VEjSdMeQcuLhw==$NrURDOCI7qMRCD0tfMAzpF2ZJFd8Pe/t3jFYOM2WByo=');
INSERT INTO preguntas_seguridad (id, usuario_id, orden, texto, hash_respuesta) VALUES (3, 2, 1, '¿Nombre de tu primera mascota?', 'pbkdf2$210000$Q9nTIrSoL/AxUr1ss399ng==$brtkCsjT8w5pMYG8oERsZ0Nhhb2hR5BIcS80T11dbOY=');
INSERT INTO preguntas_seguridad (id, usuario_id, orden, texto, hash_respuesta) VALUES (4, 2, 2, '¿Ciudad donde naciste?', 'pbkdf2$210000$ikRKwutBa6JDmyCMUQ2kEg==$zVI6ZWLPXg0ycOPhecqJlvQeMLQ8q6H2t4WYNNTh5uI=');
INSERT INTO preguntas_seguridad (id, usuario_id, orden, texto, hash_respuesta) VALUES (5, 3, 1, '¿Nombre de tu primera mascota?', 'pbkdf2$210000$KXzt6rU6Tv6LZUWveAa4Rw==$bJyUOk5SGlRAfy1hc6AkPzo91qXJP6dI/pm4byf6LEA=');
INSERT INTO preguntas_seguridad (id, usuario_id, orden, texto, hash_respuesta) VALUES (6, 3, 2, '¿Ciudad donde naciste?', 'pbkdf2$210000$XdHKBv/Tj5pEf6J5zUhFoQ==$4KuAA9GkMQDf5rpRf3G4fOKmDyFPUeVpFEXpHaTixr8=');
INSERT INTO preguntas_seguridad (id, usuario_id, orden, texto, hash_respuesta) VALUES (7, 4, 1, '¿Nombre de tu primera mascota?', 'pbkdf2$210000$OeUQbYWcaWApco8/6Rh8kQ==$LxbvUVFcHht5Kg+yt9rgb0lPKZEUt/zFv4GsH+2LvKw=');
INSERT INTO preguntas_seguridad (id, usuario_id, orden, texto, hash_respuesta) VALUES (8, 4, 2, '¿Ciudad donde naciste?', 'pbkdf2$210000$JGqtH8d8nwUnAEW5wohaOw==$uPjgogsFt2eK7Nmc8VbNGzBciDKUhpr7m6sc0SvVT6c=');
INSERT INTO preguntas_seguridad (id, usuario_id, orden, texto, hash_respuesta) VALUES (9, 5, 1, '¿Nombre de tu primera mascota?', 'pbkdf2$210000$WOZUTiPTUoGKF6KTwuQwog==$P9cZoNZjT19bHCyRE8A4mBjo3HzT3511R/v0tIr6gmI=');
INSERT INTO preguntas_seguridad (id, usuario_id, orden, texto, hash_respuesta) VALUES (10, 5, 2, '¿Ciudad donde naciste?', 'pbkdf2$210000$d8/5AXGQl1WfKMQkgIKV/g==$aDb3zgoYx0y2Dw9kDvflhJ7NvWOrq/8q8T583oNzwXY=');
INSERT INTO preguntas_seguridad (id, usuario_id, orden, texto, hash_respuesta) VALUES (11, 6, 1, '¿Nombre de tu primera mascota?', 'pbkdf2$210000$M7x8b/M79JwphEtWqy37Qg==$Wms0zmY8yYETOHpjhozxx6Ygwf8CgiQrD+PI1zOFkYQ=');
INSERT INTO preguntas_seguridad (id, usuario_id, orden, texto, hash_respuesta) VALUES (12, 6, 2, '¿Ciudad donde naciste?', 'pbkdf2$210000$OFfKUAu7bDiZD5eW/ddwCA==$V5m5QrQ9rwe88JS75nUS8OX+/YqDN2YvzysyExzInh8=');
