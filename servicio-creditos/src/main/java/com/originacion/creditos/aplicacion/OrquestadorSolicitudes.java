package com.originacion.creditos.aplicacion;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.originacion.creditos.dominio.EstadoSolicitud;
import com.originacion.creditos.dominio.PlanAmortizacion;
import com.originacion.creditos.dominio.Solicitud;
import com.originacion.creditos.dominio.SolicitudRepository;
import com.originacion.creditos.integracion.ClientesClient;
import com.originacion.creditos.integracion.EvaluacionClient;

/**
 * Coordina la llamada a otros servicios fuera de cualquier transacción, para no retener conexiones
 * de base de datos durante HTTP. Si un servicio falla, la solicitud sigue RECIBIDA y se reintenta.
 */
@Service
public class OrquestadorSolicitudes {

	private static final Logger log = LoggerFactory.getLogger(OrquestadorSolicitudes.class);

	// El endeudamiento se mide con la tasa más alta del producto para ser conservadores.
	private static final BigDecimal TASA_REFERENCIA = new BigDecimal("0.20");

	public record Creacion(Solicitud solicitud, boolean nueva) {
	}

	private final SolicitudService servicio;

	private final SolicitudRepository solicitudes;

	private final ClientesClient clientes;

	private final EvaluacionClient evaluacion;

	private final Duration antiguedadReintento;

	OrquestadorSolicitudes(SolicitudService servicio, SolicitudRepository solicitudes, ClientesClient clientes,
			EvaluacionClient evaluacion, @Value("${creditos.reintento-antiguedad}") Duration antiguedadReintento) {
		this.servicio = servicio;
		this.solicitudes = solicitudes;
		this.clientes = clientes;
		this.evaluacion = evaluacion;
		this.antiguedadReintento = antiguedadReintento;
	}

	public Creacion crear(Actor actor, String idempotencyKey, BigDecimal monto, int plazoMeses) {
		var existente = solicitudes.findByClienteIdAndIdempotencyKey(actor.clienteId(), idempotencyKey);
		if (existente.isPresent()) {
			return repetida(existente.get(), monto, plazoMeses);
		}
		Solicitud solicitud;
		try {
			solicitud = servicio.registrar(actor, idempotencyKey, monto, plazoMeses);
		}
		catch (DataIntegrityViolationException carrera) {
			return repetida(solicitudes.findByClienteIdAndIdempotencyKey(actor.clienteId(), idempotencyKey).orElseThrow(),
					monto, plazoMeses);
		}
		procesar(solicitud.getId());
		return new Creacion(solicitudes.findById(solicitud.getId()).orElseThrow(), true);
	}

	public void procesar(UUID id) {
		Solicitud solicitud = solicitudes.findById(id).orElseThrow();
		if (solicitud.getEstado() != EstadoSolicitud.RECIBIDA) {
			return;
		}
		try {
			var cliente = clientes.obtener(solicitud.getClienteId());
			if (cliente.isEmpty()) {
				servicio.rechazarPorSistema(id, "Cliente no registrado");
				return;
			}
			BigDecimal cuotaEstimada = PlanAmortizacion.cuota(solicitud.getMonto(), TASA_REFERENCIA, solicitud.getPlazoMeses());
			var datos = cliente.get();
			var resultado = evaluacion.evaluar(new EvaluacionClient.Peticion(id, datos.ingresosMensuales(),
					datos.deudaMensual(), datos.antiguedadLaboralMeses(), solicitud.getMonto(), cuotaEstimada));
			servicio.aplicarEvaluacion(id, resultado);
		}
		catch (RuntimeException fallo) {
			log.warn("Solicitud {} pendiente de reintento: {}", id, fallo.getMessage());
		}
	}

	@Scheduled(fixedDelayString = "${creditos.reintento-ms}")
	public void reintentarPendientes() {
		Instant limite = Instant.now().minus(antiguedadReintento);
		solicitudes.findTop50ByEstadoAndCreadaEnBeforeOrderByCreadaEn(EstadoSolicitud.RECIBIDA, limite)
			.forEach(solicitud -> procesar(solicitud.getId()));
	}

	private Creacion repetida(Solicitud solicitud, BigDecimal monto, int plazoMeses) {
		if (solicitud.getMonto().compareTo(monto) != 0 || solicitud.getPlazoMeses() != plazoMeses) {
			throw new IdempotenciaException();
		}
		return new Creacion(solicitud, false);
	}

}
