CREATE TABLE usuarios (
    id          UUID PRIMARY KEY,
    usuario     VARCHAR(50)  NOT NULL UNIQUE,
    clave_hash  VARCHAR(100) NOT NULL,
    rol         VARCHAR(20)  NOT NULL CHECK (rol IN ('CLIENTE', 'ANALISTA')),
    cliente_id  UUID,
    CONSTRAINT cliente_requiere_id CHECK (rol <> 'CLIENTE' OR cliente_id IS NOT NULL)
);
