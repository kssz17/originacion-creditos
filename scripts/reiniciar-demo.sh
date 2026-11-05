#!/usr/bin/env bash
# Borra solicitudes, evaluaciones y pagos y genera una cartera con seis meses de historia.
# No toca usuarios ni clientes. Las fechas pasadas se ajustan con SQL: es solo para la demo.
# Uso: ./scripts/reiniciar-demo.sh
set -euo pipefail

API=${API:-http://localhost:8080/api}

sql() { docker compose exec -T postgres psql -q -U postgres -d "$1" -c "$2"; }
campo() { node -pe "JSON.parse(require('fs').readFileSync(0))$1"; }

token() {
  curl -s -X POST "$API/auth/demo" -H 'Content-Type: application/json' -d "{\"usuario\":\"$1\"}" | campo .token
}

solicitar() { # token, monto, plazo
  curl -s -X POST "$API/solicitudes" -H "Authorization: Bearer $1" -H "Idempotency-Key: demo-$RANDOM$RANDOM" \
    -H 'Content-Type: application/json' -d "{\"monto\":$2,\"plazoMeses\":$3}"
}

desembolsar() { # id
  curl -s -o /dev/null -X POST "$API/solicitudes/$1/desembolso" -H "Authorization: Bearer $JORGE"
}

retrasar() { # id, meses
  sql creditos "UPDATE solicitudes SET desembolsada_en = current_date - make_interval(months => $2),
    creada_en = now() - make_interval(months => $2) WHERE id = '$1';
    UPDATE cuotas c SET vencimiento = (s.desembolsada_en + make_interval(months => c.numero))::date
    FROM solicitudes s WHERE s.id = c.solicitud_id AND s.id = '$1'"
}

pagar() { # token, id, veces
  for _ in $(seq 1 "$3"); do
    curl -s -o /dev/null -X POST "$API/solicitudes/$2/pagos" -H "Authorization: Bearer $1" \
      -H "Idempotency-Key: demo-$RANDOM$RANDOM" -H 'Content-Type: application/json' -d '{"tipo":"CUOTA"}'
  done
}

# TRUNCATE no dispara el trigger de auditoría, que solo protege filas sueltas.
sql creditos 'TRUNCATE auditoria, factores_score, pagos, cuotas, solicitudes'
sql evaluacion 'TRUNCATE evaluacion_factores, evaluaciones'

ANA=$(token ana); LUIS=$(token luis); CARLA=$(token carla); MARIA=$(token maria); JORGE=$(token jorge)

# Crédito con dos cuotas vencidas: muestra la mora en el panel del cliente y del analista.
ID=$(solicitar "$ANA" 15000 48 | campo .id)
desembolsar "$ID"; retrasar "$ID" 5; pagar "$ANA" "$ID" 2

# Crédito al día.
ID=$(solicitar "$ANA" 8000 24 | campo .id)
desembolsar "$ID"; retrasar "$ID" 3; pagar "$ANA" "$ID" 3

ID=$(solicitar "$ANA" 20000 60 | campo .id)
desembolsar "$ID"; retrasar "$ID" 2; pagar "$ANA" "$ID" 2

# Crédito ya liquidado.
ID=$(solicitar "$ANA" 12000 36 | campo .id)
desembolsar "$ID"; retrasar "$ID" 4
curl -s -o /dev/null -X POST "$API/solicitudes/$ID/pagos" -H "Authorization: Bearer $ANA" \
  -H "Idempotency-Key: demo-$RANDOM$RANDOM" -H 'Content-Type: application/json' -d '{"tipo":"LIQUIDACION"}'

# Aprobado por una analista y desembolsado por otro.
ID=$(solicitar "$LUIS" 6000 24 | campo .id)
curl -s -o /dev/null -X POST "$API/solicitudes/$ID/decision" -H "Authorization: Bearer $MARIA" \
  -H 'Content-Type: application/json' -d '{"aprobada":true,"motivo":"Ingresos verificados"}'
desembolsar "$ID"; retrasar "$ID" 1; pagar "$LUIS" "$ID" 1

solicitar "$ANA" 5000 24 | campo .estado
solicitar "$LUIS" 12000 36 | campo .estado
solicitar "$CARLA" 8000 24 | campo .estado
