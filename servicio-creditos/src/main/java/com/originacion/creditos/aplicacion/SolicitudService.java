package com.originacion.creditos.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.originacion.creditos.dominio.AuditoriaRepository;
import com.originacion.creditos.dominio.Cuota;
import com.originacion.creditos.dominio.CuotaRepository;
import com.originacion.creditos.dominio.EstadoSolicitud;
import com.originacion.creditos.dominio.EventoAuditoria;
import com.originacion.creditos.dominio.FactorScore;
import com.originacion.creditos.dominio.FactorScoreRepository;
import com.originacion.creditos.dominio.PlanAmortizacion;
import com.originacion.creditos.dominio.ReglaNegocioException;
import com.originacion.creditos.dominio.Solicitud;
import com.originacion.creditos.dominio.SolicitudRepository;
import com.originacion.creditos.integracion.EvaluacionClient;

/** Cada método es una transacción local: el cambio de estado y su auditoría se confirman juntos. */
@Service
public class SolicitudService {

	private static final String SISTEMA = "sistema";

	private final SolicitudRepository solicitudes;

	private final CuotaRepository cuotas;

	private final AuditoriaRepository auditoria;

	private final FactorScoreRepository factores;

	private final Clock reloj = Clock.systemDefaultZone();

	SolicitudService(SolicitudRepository solicitudes, CuotaRepository cuotas, AuditoriaRepository auditoria,
			FactorScoreRepository factores) {
		this.solicitudes = solicitudes;
		this.cuotas = cuotas;
		this.auditoria = auditoria;
		this.factores = factores;
	}

	@Transactional
	public Solicitud registrar(Actor actor, String idempotencyKey, BigDecimal monto, int plazoMeses) {
		Solicitud solicitud = solicitudes.saveAndFlush(new Solicitud(actor.clienteId(), idempotencyKey, monto, plazoMeses));
		auditar(solicitud, null, actor.usuario(), "Solicitud registrada");
		return solicitud;
	}

	/** Si otra ejecución ya procesó la solicitud no hace nada, de modo que los reintentos son seguros. */
	@Transactional
	public void aplicarEvaluacion(UUID id, EvaluacionClient.Resultado resultado) {
		Solicitud solicitud = solicitudes.findById(id).orElseThrow();
		if (solicitud.getEstado() != EstadoSolicitud.RECIBIDA) {
			return;
		}
		solicitud.registrarEvaluacion(resultado.score(), resultado.dti(), resultado.tasaAnual(), resultado.motivo());
		guardarFactores(id, resultado);
		switch (resultado.decision()) {
			case "APROBADA" -> aprobar(solicitud, SISTEMA, resultado.motivo());
			case "REVISION_MANUAL" -> cambiarEstado(solicitud, EstadoSolicitud.EN_REVISION, SISTEMA, resultado.motivo());
			default -> cambiarEstado(solicitud, EstadoSolicitud.RECHAZADA, SISTEMA, resultado.motivo());
		}
	}

	@Transactional
	public void rechazarPorSistema(UUID id, String motivo) {
		Solicitud solicitud = solicitudes.findById(id).orElseThrow();
		if (solicitud.getEstado() == EstadoSolicitud.RECIBIDA) {
			solicitud.fijarMotivo(motivo);
			cambiarEstado(solicitud, EstadoSolicitud.RECHAZADA, SISTEMA, motivo);
		}
	}

	@Transactional
	public Solicitud decidir(UUID id, Actor analista, boolean aprobada, String motivo) {
		Solicitud solicitud = solicitudes.findById(id).orElseThrow(SolicitudNoEncontradaException::new);
		if (solicitud.getEstado() != EstadoSolicitud.EN_REVISION) {
			throw new ReglaNegocioException("Solo se deciden solicitudes en revisión");
		}
		solicitud.registrarRevision(analista.usuario(), motivo);
		if (aprobada) {
			aprobar(solicitud, analista.usuario(), motivo);
		}
		else {
			cambiarEstado(solicitud, EstadoSolicitud.RECHAZADA, analista.usuario(), motivo);
		}
		return solicitud;
	}

	/** Segregación de funciones: quien aprobó manualmente no puede desembolsar el mismo crédito. */
	@Transactional
	public Solicitud desembolsar(UUID id, Actor analista) {
		Solicitud solicitud = solicitudes.findById(id).orElseThrow(SolicitudNoEncontradaException::new);
		if (analista.usuario().equals(solicitud.getAnalista())) {
			throw new ReglaNegocioException("El desembolso requiere un analista distinto al que aprobó");
		}
		EstadoSolicitud anterior = solicitud.getEstado();
		LocalDate fecha = LocalDate.now(reloj);
		solicitud.desembolsar(fecha);
		cuotas.findBySolicitudIdOrderByNumero(id).forEach(cuota -> cuota.fijarVencimiento(fecha.plusMonths(cuota.getNumero())));
		auditar(solicitud, anterior, analista.usuario(), "Crédito desembolsado");
		return solicitud;
	}

	private void guardarFactores(UUID id, EvaluacionClient.Resultado resultado) {
		var lista = resultado.factores();
		for (int orden = 0; orden < lista.size(); orden++) {
			var factor = lista.get(orden);
			factores.save(new FactorScore(id, orden, factor.nombre(), factor.puntos(), factor.maximo(), factor.detalle()));
		}
	}

	private void aprobar(Solicitud solicitud, String actor, String detalle) {
		var plan = PlanAmortizacion.generar(solicitud.getMonto(), solicitud.getTasaAnual(), solicitud.getPlazoMeses());
		solicitud.fijarCuota(plan.getFirst().total());
		cuotas.saveAll(plan.stream().map(fila -> new Cuota(solicitud.getId(), fila)).toList());
		cambiarEstado(solicitud, EstadoSolicitud.APROBADA, actor, detalle);
	}

	private void cambiarEstado(Solicitud solicitud, EstadoSolicitud destino, String actor, String detalle) {
		EstadoSolicitud anterior = solicitud.getEstado();
		solicitud.transicionar(destino);
		auditar(solicitud, anterior, actor, detalle);
	}

	private void auditar(Solicitud solicitud, EstadoSolicitud anterior, String actor, String detalle) {
		auditoria.save(new EventoAuditoria(solicitud.getId(), anterior, solicitud.getEstado(), actor, detalle));
	}

}
