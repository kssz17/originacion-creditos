CREATE TABLE evaluaciones (
    id           UUID PRIMARY KEY,
    solicitud_id UUID          NOT NULL UNIQUE,
    score        INTEGER       NOT NULL,
    dti          NUMERIC(6, 4) NOT NULL,
    decision     VARCHAR(20)   NOT NULL CHECK (decision IN ('APROBADA', 'REVISION_MANUAL', 'RECHAZADA')),
    tasa_anual   NUMERIC(6, 4),
    motivo       VARCHAR(300)  NOT NULL,
    creado_en    TIMESTAMPTZ   NOT NULL
);
