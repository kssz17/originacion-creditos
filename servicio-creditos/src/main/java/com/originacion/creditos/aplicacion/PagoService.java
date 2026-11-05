package com.originacion.creditos.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.originacion.creditos.dominio.AuditoriaRepository;
import com.originacion.creditos.dominio.Cuota;
import com.originacion.creditos.dominio.CuotaRepository;
import com.originacion.creditos.dominio.EstadoSolicitud;
import com.originacion.creditos.dominio.EventoAuditoria;
import com.originacion.creditos.dominio.Pago;
import com.originacion.creditos.dominio.PagoRepository;
import com.originacion.creditos.dominio.ReglaNegocioException;
import com.originacion.creditos.dominio.Solicitud;
import com.originacion.creditos.dominio.SolicitudRepository;
import com.originacion.creditos.dominio.TipoPago;

/**
 * Los pagos son simulados: no hay pasarela ni movimiento real de dinero. El crédito se bloquea con un
 * lock pesimista porque dos pagos simultáneos podrían cobrar la misma cuota, y aquí un conflicto
 * sería caro de reintentar.
 */
@Service
public class PagoService {

	public record Resultado(Pago pago, boolean nuevo) {
	}

	private final SolicitudRepository solicitudes;

	private final CuotaRepository cuotas;

	private final PagoRepository pagos;

	private final AuditoriaRepository auditoria;

	private final Clock reloj = Clock.systemDefaultZone();

	PagoService(SolicitudRepository solicitudes, CuotaRepository cuotas, PagoRepository pagos,
			AuditoriaRepository auditoria) {
		this.solicitudes = solicitudes;
		this.cuotas = cuotas;
		this.pagos = pagos;
		this.auditoria = auditoria;
	}

	@Transactional
	public Resultado pagar(UUID id, Actor actor, TipoPago tipo, String idempotencyKey) {
		Solicitud solicitud = solicitudes.bloquear(id).orElseThrow(SolicitudNoEncontradaException::new);
		if (!solicitud.getClienteId().equals(actor.clienteId())) {
			throw new SolicitudNoEncontradaException();
		}
		var repetido = pagos.findBySolicitudIdAndIdempotencyKey(id, idempotencyKey);
		if (repetido.isPresent()) {
			if (repetido.get().getTipo() != tipo) {
				throw new IdempotenciaException();
			}
			return new Resultado(repetido.get(), false);
		}
		if (solicitud.getEstado() != EstadoSolicitud.DESEMBOLSADA) {
			throw new ReglaNegocioException("Solo se pagan créditos desembolsados");
		}
		List<Cuota> pendientes = cuotas.findBySolicitudIdAndPagadaEnIsNullOrderByNumero(id);
		Pago pago = pagos.save(tipo == TipoPago.CUOTA ? pagarCuota(id, idempotencyKey, pendientes.getFirst())
				: liquidar(id, idempotencyKey, pendientes));
		auditar(id, EstadoSolicitud.DESEMBOLSADA, actor, descripcion(pago));
		if (pago.getSaldoRestante().signum() == 0) {
			solicitud.liquidar();
			auditar(id, EstadoSolicitud.LIQUIDADA, actor, "Crédito liquidado");
		}
		return new Resultado(pago, true);
	}

	private Pago pagarCuota(UUID id, String idempotencyKey, Cuota cuota) {
		LocalDate hoy = LocalDate.now(reloj);
		Instant ahora = Instant.now(reloj);
		Pago pago = new Pago(id, idempotencyKey, TipoPago.CUOTA, cuota.getCapital(), cuota.getInteres(),
				cuota.recargo(hoy), cuota.getSaldo(), ahora);
		cuota.marcarPagada(ahora);
		return pago;
	}

	/** Solo se cobra el interés ya vencido: liquidar antes de tiempo evita el de las cuotas futuras. */
	private Pago liquidar(UUID id, String idempotencyKey, List<Cuota> pendientes) {
		LocalDate hoy = LocalDate.now(reloj);
		Instant ahora = Instant.now(reloj);
		BigDecimal capital = BigDecimal.ZERO;
		BigDecimal interes = BigDecimal.ZERO;
		BigDecimal recargo = BigDecimal.ZERO;
		for (Cuota cuota : pendientes) {
			capital = capital.add(cuota.getCapital());
			if (cuota.estaVencida(hoy)) {
				interes = interes.add(cuota.getInteres());
				recargo = recargo.add(cuota.recargo(hoy));
			}
			cuota.marcarPagada(ahora);
		}
		return new Pago(id, idempotencyKey, TipoPago.LIQUIDACION, capital, interes, recargo, BigDecimal.ZERO, ahora);
	}

	private String descripcion(Pago pago) {
		String concepto = pago.getTipo() == TipoPago.CUOTA ? "Pago de cuota" : "Pago de liquidación";
		return "%s por %s €".formatted(concepto, pago.getTotal().toPlainString().replace('.', ','));
	}

	private void auditar(UUID id, EstadoSolicitud nuevo, Actor actor, String detalle) {
		auditoria.save(new EventoAuditoria(id, EstadoSolicitud.DESEMBOLSADA, nuevo, actor.usuario(), detalle));
	}

}
