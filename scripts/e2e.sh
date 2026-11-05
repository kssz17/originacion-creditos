#!/usr/bin/env bash
# Recorre el flujo completo contra el sistema levantado con docker compose.
# Uso: DEMO_CLAVE=... ./scripts/e2e.sh
set -euo pipefail

API=${API:-http://localhost:8080/api}
CLAVE=${DEMO_CLAVE:?Define DEMO_CLAVE}
fallos=0

campo() { node -pe "JSON.parse(require('fs').readFileSync(0))$1"; }

comprobar() { # descripción, esperado, obtenido
  if [ "$2" = "$3" ]; then
    echo "OK    $1"
  else
    echo "FALLO $1 (esperado: $2, obtenido: $3)"
    fallos=$((fallos + 1))
  fi
}

token() {
  curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
    -d "{\"usuario\":\"$1\",\"clave\":\"$CLAVE\"}" | campo .token
}

solicitar() { # token, clave de idempotencia, monto, plazo
  curl -s -X POST "$API/solicitudes" -H "Authorization: Bearer $1" -H "Idempotency-Key: $2" \
    -H 'Content-Type: application/json' -d "{\"monto\":$3,\"plazoMeses\":$4}"
}

codigo() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

ANA=$(token ana); LUIS=$(token luis); CARLA=$(token carla); MARIA=$(token maria); JORGE=$(token jorge)
AUTO=e2e-$RANDOM$RANDOM

comprobar "sin token responde 401" 401 "$(codigo "$API/solicitudes")"
comprobar "clave incorrecta responde 401" 401 "$(codigo -X POST "$API/auth/login" -H 'Content-Type: application/json' -d '{"usuario":"ana","clave":"mala"}')"

r=$(solicitar "$ANA" "$AUTO-a" 10000 36)
ID_ANA=$(echo "$r" | campo .id)
comprobar "perfil sólido se aprueba solo" APROBADA "$(echo "$r" | campo .estado)"
comprobar "repetir la petición devuelve la misma solicitud" "$ID_ANA" "$(solicitar "$ANA" "$AUTO-a" 10000 36 | campo .id)"
comprobar "misma clave con otro monto responde 409" 409 "$(codigo -X POST "$API/solicitudes" -H "Authorization: Bearer $ANA" -H "Idempotency-Key: $AUTO-a" -H 'Content-Type: application/json' -d '{"monto":20000,"plazoMeses":36}')"

comprobar "alto endeudamiento se rechaza" RECHAZADA "$(solicitar "$CARLA" "$AUTO-c" 8000 24 | campo .estado)"

r=$(solicitar "$LUIS" "$AUTO-l" 12000 36)
ID_LUIS=$(echo "$r" | campo .id)
comprobar "perfil intermedio pasa a revisión" EN_REVISION "$(echo "$r" | campo .estado)"

comprobar "un cliente no ve solicitudes ajenas" 404 "$(codigo "$API/solicitudes/$ID_LUIS" -H "Authorization: Bearer $ANA")"
comprobar "un cliente no puede decidir" 403 "$(codigo -X POST "$API/solicitudes/$ID_LUIS/decision" -H "Authorization: Bearer $LUIS" -H 'Content-Type: application/json' -d '{"aprobada":true,"motivo":"x"}')"
comprobar "cabeceras falsificadas no suplantan a un analista" 403 "$(codigo -X POST "$API/solicitudes/$ID_LUIS/decision" -H "Authorization: Bearer $LUIS" -H 'X-Rol: ANALISTA' -H 'X-Usuario: maria' -H 'Content-Type: application/json' -d '{"aprobada":true,"motivo":"x"}')"
comprobar "las rutas internas no se exponen" 404 "$(codigo "$API/interno/clientes/$ID_LUIS" -H "Authorization: Bearer $MARIA")"

comprobar "la analista aprueba" 200 "$(codigo -X POST "$API/solicitudes/$ID_LUIS/decision" -H "Authorization: Bearer $MARIA" -H 'Content-Type: application/json' -d '{"aprobada":true,"motivo":"Ingresos verificados"}')"
comprobar "quien aprobó no desembolsa" 409 "$(codigo -X POST "$API/solicitudes/$ID_LUIS/desembolso" -H "Authorization: Bearer $MARIA")"
comprobar "otro analista desembolsa" DESEMBOLSADA "$(curl -s -X POST "$API/solicitudes/$ID_LUIS/desembolso" -H "Authorization: Bearer $JORGE" | campo .estado)"

detalle=$(curl -s "$API/solicitudes/$ID_LUIS" -H "Authorization: Bearer $MARIA")
comprobar "el plan tiene 36 cuotas" 36 "$(echo "$detalle" | campo .cuotas.length)"
comprobar "el saldo final es cero" 0 "$(echo "$detalle" | campo '.cuotas[35].saldo')"
comprobar "la auditoría tiene cuatro eventos" 4 "$(echo "$detalle" | campo .auditoria.length)"
comprobar "la auditoría guarda el correlation id" 36 "$(echo "$detalle" | campo '.auditoria[3].correlationId.length')"

if command -v docker > /dev/null; then
  docker compose stop servicio-evaluacion > /dev/null 2>&1
  r=$(solicitar "$ANA" "$AUTO-r" 5000 12)
  ID_R=$(echo "$r" | campo .id)
  comprobar "con evaluación caída queda RECIBIDA" RECIBIDA "$(echo "$r" | campo .estado)"
  docker compose start servicio-evaluacion > /dev/null 2>&1
  estado=RECIBIDA
  for _ in $(seq 1 45); do
    estado=$(curl -s "$API/solicitudes/$ID_R" -H "Authorization: Bearer $ANA" | campo .solicitud.estado)
    [ "$estado" = RECIBIDA ] || break
    sleep 4
  done
  comprobar "el reintento la resuelve al volver el servicio" APROBADA "$estado"
fi

echo "Fallos: $fallos"
exit "$fallos"
