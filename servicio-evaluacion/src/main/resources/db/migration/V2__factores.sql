CREATE TABLE evaluacion_factores (
    evaluacion_id UUID         NOT NULL REFERENCES evaluaciones (id),
    orden         INTEGER      NOT NULL,
    nombre        VARCHAR(60)  NOT NULL,
    puntos        INTEGER      NOT NULL,
    maximo        INTEGER      NOT NULL,
    detalle       VARCHAR(120) NOT NULL,
    PRIMARY KEY (evaluacion_id, orden)
);
