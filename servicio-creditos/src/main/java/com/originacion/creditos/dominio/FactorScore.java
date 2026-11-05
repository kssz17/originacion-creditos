package com.originacion.creditos.dominio;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "factores_score")
@IdClass(FactorScore.Clave.class)
public class FactorScore {

	public record Clave(UUID solicitudId, int orden) implements Serializable {

		public Clave() {
			this(null, 0);
		}

	}

	@Id
	@Column(name = "solicitud_id")
	private UUID solicitudId;

	@Id
	private int orden;

	@Column(nullable = false)
	private String nombre;

	@Column(nullable = false)
	private int puntos;

	@Column(nullable = false)
	private int maximo;

	@Column(nullable = false)
	private String detalle;

	protected FactorScore() {
	}

	public FactorScore(UUID solicitudId, int orden, String nombre, int puntos, int maximo, String detalle) {
		this.solicitudId = solicitudId;
		this.orden = orden;
		this.nombre = nombre;
		this.puntos = puntos;
		this.maximo = maximo;
		this.detalle = detalle;
	}

	public String getNombre() {
		return nombre;
	}

	public int getPuntos() {
		return puntos;
	}

	public int getMaximo() {
		return maximo;
	}

	public String getDetalle() {
		return detalle;
	}

}
