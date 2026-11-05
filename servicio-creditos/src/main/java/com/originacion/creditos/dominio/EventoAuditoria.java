package com.originacion.creditos.dominio;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.MDC;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auditoria")
public class EventoAuditoria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "solicitud_id", nullable = false)
	private UUID solicitudId;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_anterior")
	private EstadoSolicitud estadoAnterior;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_nuevo", nullable = false)
	private EstadoSolicitud estadoNuevo;

	@Column(nullable = false)
	private String actor;

	private String detalle;

	@Column(name = "correlation_id")
	private String correlationId;

	@Column(name = "ocurrido_en", nullable = false)
	private Instant ocurridoEn;

	protected EventoAuditoria() {
	}

	public EventoAuditoria(UUID solicitudId, EstadoSolicitud estadoAnterior, EstadoSolicitud estadoNuevo, String actor,
			String detalle) {
		this.solicitudId = solicitudId;
		this.estadoAnterior = estadoAnterior;
		this.estadoNuevo = estadoNuevo;
		this.actor = actor;
		this.detalle = detalle;
		this.correlationId = MDC.get("correlationId");
		this.ocurridoEn = Instant.now();
	}

	public UUID getSolicitudId() {
		return solicitudId;
	}

	public EstadoSolicitud getEstadoAnterior() {
		return estadoAnterior;
	}

	public EstadoSolicitud getEstadoNuevo() {
		return estadoNuevo;
	}

	public String getActor() {
		return actor;
	}

	public String getDetalle() {
		return detalle;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public Instant getOcurridoEn() {
		return ocurridoEn;
	}

}
