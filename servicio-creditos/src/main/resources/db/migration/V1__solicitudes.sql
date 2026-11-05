CREATE TABLE solicitudes (
    id              UUID PRIMARY KEY,
    cliente_id      UUID           NOT NULL,
    idempotency_key VARCHAR(64)    NOT NULL,
    monto           NUMERIC(14, 2) NOT NULL CHECK (monto > 0),
    plazo_meses     INTEGER        NOT NULL CHECK (plazo_meses > 0),
    estado          VARCHAR(20)    NOT NULL
        CHECK (estado IN ('RECIBIDA', 'EN_REVISION', 'APROBADA', 'RECHAZADA', 'DESEMBOLSADA')),
    score           INTEGER,
    dti             NUMERIC(6, 4),
    tasa_anual      NUMERIC(6, 4),
    cuota_mensual   NUMERIC(14, 2),
    motivo          VARCHAR(300),
    analista        VARCHAR(50),
    creada_en       TIMESTAMPTZ    NOT NULL,
    desembolsada_en DATE,
    version         BIGINT         NOT NULL,
    CONSTRAINT solicitud_idempotente UNIQUE (cliente_id, idempotency_key)
);

CREATE INDEX idx_solicitudes_estado ON solicitudes (estado, creada_en);
CREATE INDEX idx_solicitudes_cliente ON solicitudes (cliente_id, creada_en DESC);

CREATE TABLE cuotas (
    solicitud_id UUID           NOT NULL REFERENCES solicitudes (id),
    numero       INTEGER        NOT NULL,
    capital      NUMERIC(14, 2) NOT NULL,
    interes      NUMERIC(14, 2) NOT NULL,
    total        NUMERIC(14, 2) NOT NULL,
    saldo        NUMERIC(14, 2) NOT NULL,
    PRIMARY KEY (solicitud_id, numero)
);

CREATE TABLE auditoria (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    solicitud_id    UUID         NOT NULL REFERENCES solicitudes (id),
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20)  NOT NULL,
    actor           VARCHAR(50)  NOT NULL,
    detalle         VARCHAR(300),
    correlation_id  VARCHAR(64),
    ocurrido_en     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_auditoria_solicitud ON auditoria (solicitud_id, id);

-- La auditoría es de solo inserción: la propia base de datos impide modificarla.
CREATE FUNCTION rechazar_cambio_auditoria() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'La tabla auditoria es de solo inserción';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER auditoria_inmutable
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION rechazar_cambio_auditoria();
