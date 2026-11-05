ALTER TABLE usuarios ADD COLUMN nombre VARCHAR(120), ADD COLUMN email VARCHAR(120);

UPDATE usuarios SET nombre = initcap(usuario), email = usuario || '@demo.test';

ALTER TABLE usuarios ALTER COLUMN nombre SET NOT NULL, ALTER COLUMN email SET NOT NULL;
