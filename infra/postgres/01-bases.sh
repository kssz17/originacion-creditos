#!/bin/sh
# Una base de datos y un usuario por servicio: ninguno puede leer los datos de otro.
set -eu
for servicio in identidad clientes evaluacion creditos; do
  psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" <<SQL
CREATE USER $servicio WITH PASSWORD '$DB_CLAVE_SERVICIOS';
CREATE DATABASE $servicio OWNER $servicio;
REVOKE CONNECT ON DATABASE $servicio FROM PUBLIC;
SQL
done
