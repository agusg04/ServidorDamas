-- Crear el usuario 'damas' con la contraseña 'damas' si no existe
CREATE USER IF NOT EXISTS 'damas'@'%' IDENTIFIED BY 'damas';

-- Otorgar permisos CRUD al usuario 'damas' en la base de datos 'damas'
GRANT SELECT, INSERT, UPDATE, DELETE ON damas.* TO 'damas'@'%';

-- Aplicar los cambios
FLUSH PRIVILEGES;