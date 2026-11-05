package com.originacion.creditos.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.originacion.creditos.dominio.Cuota;
import com.originacion.creditos.dominio.EstadoSolicitud;
import com.originacion.creditos.dominio.EventoAuditoria;
import com.originacion.creditos.dominio.FactorScore;
import com.originacion.creditos.dominio.Pago;
import com.originacion.creditos.dominio.Solicitud;
import com.originacion.creditos.dominio.TipoPago;

final class Respuestas {

	record SolicitudDto(UUID id, UUID clienteId, BigDecimal monto, int plazoMeses, EstadoSolicitud estado, Integer score,
			BigDecimal dti, BigDecimal tasaAnual, BigDecimal cuotaMensual, String motivo, String analista, Instant creadaEn,
			LocalDate desembolsadaEn) {

		static SolicitudDto de(Solicitud s) {
			return new SolicitudDto(s.getId(), s.getClienteId(), s.getMonto(), s.getPlazoMeses(), s.getEstado(),
					s.getScore(), s.getDti(), s.getTasaAnual(), s.getCuotaMensual(), s.getMotivo(), s.getAnalista(),
					s.getCreadaEn(), s.getDesembolsadaEn());
		}

	}

	enum EstadoCuota {

		PENDIENTE, VENCIDA, PAGADA

	}

	record CuotaDto(int numero, LocalDate vencimiento, BigDecimal capital, BigDecimal interes, BigDecimal total,
			BigDecimal saldo, EstadoCuota estado, BigDecimal recargo) {

		static CuotaDto de(Cuota c, LocalDate hoy) {
			EstadoCuota estado = c.getPagadaEn() != null ? EstadoCuota.PAGADA
					: c.estaVencida(hoy) ? EstadoCuota.VENCIDA : EstadoCuota.PENDIENTE;
			return new CuotaDto(c.getNumero(), c.getVencimiento(), c.getCapital(), c.getInteres(), c.getTotal(),
					c.getSaldo(), estado, c.recargo(hoy));
		}

	}

	record PagoDto(UUID id, TipoPago tipo, BigDecimal capital, BigDecimal interes, BigDecimal recargo, BigDecimal total,
			BigDecimal saldoRestante, Instant creadoEn) {

		static PagoDto de(Pago p) {
			return new PagoDto(p.getId(), p.getTipo(), p.getCapital(), p.getInteres(), p.getRecargo(), p.getTotal(),
					p.getSaldoRestante(), p.getCreadoEn());
		}

	}

	record FactorDto(String nombre, int puntos, int maximo, String detalle) {

		static FactorDto de(FactorScore f) {
			return new FactorDto(f.getNombre(), f.getPuntos(), f.getMaximo(), f.getDetalle());
		}

	}

	record EventoDto(EstadoSolicitud estadoAnterior, EstadoSolicitud estadoNuevo, String actor, String detalle,
			String correlationId, Instant ocurridoEn) {

		static EventoDto de(EventoAuditoria e) {
			return new EventoDto(e.getEstadoAnterior(), e.getEstadoNuevo(), e.getActor(), e.getDetalle(),
					e.getCorrelationId(), e.getOcurridoEn());
		}

	}

	record DetalleDto(SolicitudDto solicitud, List<CuotaDto> cuotas, List<EventoDto> auditoria, List<PagoDto> pagos,
			List<FactorDto> factores, BigDecimal saldoPendiente, BigDecimal importeLiquidacion) {
	}

	record MesDto(String mes, BigDecimal monto, long creditos) {
	}

	record AnaliticaDto(long recibidas, long evaluadas, long aprobadas, long desembolsadas, BigDecimal tasaAprobacion,
			List<MesDto> desembolsadoPorMes, BigDecimal saldoActivo, BigDecimal saldoEnMora, long creditosEnMora,
			BigDecimal ratioMora) {
	}

	record OfertaDto(int plazoMeses, BigDecimal tasaDesde, BigDecimal tasaHasta, BigDecimal cuotaDesde,
			BigDecimal cuotaHasta, BigDecimal interesDesde) {
	}

	record ResumenDto(Map<EstadoSolicitud, Long> porEstado, BigDecimal montoDesembolsado, BigDecimal cuotaMensual,
			BigDecimal ultimoDti, BigDecimal saldoPendiente, long cuotasVencidas, LocalDate proximoVencimiento,
			BigDecimal proximaCuota) {
	}

	/** El actor se omite para clientes: no necesitan saber qué analista revisó su solicitud. */
	record MovimientoDto(UUID solicitudId, EstadoSolicitud estado, String actor, boolean propio, String detalle,
			Instant ocurridoEn) {

		static MovimientoDto de(EventoAuditoria e, boolean incluirActor, String usuario) {
			return new MovimientoDto(e.getSolicitudId(), e.getEstadoNuevo(), incluirActor ? e.getActor() : null,
					e.getActor().equals(usuario), e.getDetalle(), e.getOcurridoEn());
		}

	}

	private Respuestas() {
	}

}
