CREATE TABLE clientes (
    id                       UUID PRIMARY KEY,
    documento                VARCHAR(20)    NOT NULL UNIQUE,
    nombre                   VARCHAR(120)   NOT NULL,
    email                    VARCHAR(120)   NOT NULL,
    ingresos_mensuales       NUMERIC(14, 2) NOT NULL CHECK (ingresos_mensuales > 0),
    deuda_mensual            NUMERIC(14, 2) NOT NULL CHECK (deuda_mensual >= 0),
    antiguedad_laboral_meses INTEGER        NOT NULL CHECK (antiguedad_laboral_meses >= 0),
    creado_en                TIMESTAMPTZ    NOT NULL
);
