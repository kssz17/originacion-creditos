package com.originacion.evaluacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "evaluaciones")
public class Evaluacion {

	@Id
	private UUID id;

	@Column(name = "solicitud_id", nullable = false, unique = true)
	private UUID solicitudId;

	@Column(nullable = false)
	private int score;

	@Column(nullable = false)
	private BigDecimal dti;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Decision decision;

	@Column(name = "tasa_anual")
	private BigDecimal tasaAnual;

	@Column(nullable = false)
	private String motivo;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "evaluacion_factores", joinColumns = @JoinColumn(name = "evaluacion_id"))
	@OrderColumn(name = "orden")
	private List<Factor> factores = new ArrayList<>();

	protected Evaluacion() {
	}

	public Evaluacion(UUID solicitudId, MotorScoring.Resultado resultado) {
		this.id = UUID.randomUUID();
		this.solicitudId = solicitudId;
		this.score = resultado.score();
		this.dti = resultado.dti();
		this.decision = resultado.decision();
		this.tasaAnual = resultado.tasaAnual();
		this.motivo = resultado.motivo();
		this.creadoEn = Instant.now();
		this.factores.addAll(resultado.factores());
	}

	public UUID getSolicitudId() {
		return solicitudId;
	}

	public int getScore() {
		return score;
	}

	public BigDecimal getDti() {
		return dti;
	}

	public Decision getDecision() {
		return decision;
	}

	public BigDecimal getTasaAnual() {
		return tasaAnual;
	}

	public List<Factor> getFactores() {
		return factores;
	}

	public String getMotivo() {
		return motivo;
	}

}
