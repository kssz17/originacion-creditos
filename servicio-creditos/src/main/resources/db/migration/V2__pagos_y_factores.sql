ALTER TABLE solicitudes DROP CONSTRAINT solicitudes_estado_check;
ALTER TABLE solicitudes ADD CONSTRAINT solicitudes_estado_check
    CHECK (estado IN ('RECIBIDA', 'EN_REVISION', 'APROBADA', 'RECHAZADA', 'DESEMBOLSADA', 'LIQUIDADA'));

ALTER TABLE cuotas ADD COLUMN vencimiento DATE, ADD COLUMN pagada_en TIMESTAMPTZ;

UPDATE cuotas c
SET vencimiento = (s.desembolsada_en + make_interval(months => c.numero))::date
FROM solicitudes s
WHERE s.id = c.solicitud_id AND s.desembolsada_en IS NOT NULL;

CREATE INDEX idx_cuotas_pendientes ON cuotas (vencimiento) WHERE pagada_en IS NULL;

CREATE TABLE pagos (
    id              UUID PRIMARY KEY,
    solicitud_id    UUID           NOT NULL REFERENCES solicitudes (id),
    idempotency_key VARCHAR(64)    NOT NULL,
    tipo            VARCHAR(20)    NOT NULL CHECK (tipo IN ('CUOTA', 'LIQUIDACION')),
    capital         NUMERIC(14, 2) NOT NULL,
    interes         NUMERIC(14, 2) NOT NULL,
    recargo         NUMERIC(14, 2) NOT NULL,
    total           NUMERIC(14, 2) NOT NULL,
    saldo_restante  NUMERIC(14, 2) NOT NULL,
    creado_en       TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pago_idempotente UNIQUE (solicitud_id, idempotency_key)
);

CREATE TABLE factores_score (
    solicitud_id UUID         NOT NULL REFERENCES solicitudes (id),
    orden        INTEGER      NOT NULL,
    nombre       VARCHAR(60)  NOT NULL,
    puntos       INTEGER      NOT NULL,
    maximo       INTEGER      NOT NULL,
    detalle      VARCHAR(120) NOT NULL,
    PRIMARY KEY (solicitud_id, orden)
);
