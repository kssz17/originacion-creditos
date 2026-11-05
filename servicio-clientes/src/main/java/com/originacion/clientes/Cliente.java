package com.originacion.clientes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clientes")
public class Cliente {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private String documento;

	@Column(nullable = false)
	private String nombre;

	@Column(nullable = false)
	private String email;

	@Column(name = "ingresos_mensuales", nullable = false)
	private BigDecimal ingresosMensuales;

	@Column(name = "deuda_mensual", nullable = false)
	private BigDecimal deudaMensual;

	@Column(name = "antiguedad_laboral_meses", nullable = false)
	private int antiguedadLaboralMeses;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	protected Cliente() {
	}

	public Cliente(String documento, String nombre, String email, BigDecimal ingresosMensuales, BigDecimal deudaMensual,
			int antiguedadLaboralMeses) {
		this.id = UUID.randomUUID();
		this.documento = documento;
		this.nombre = nombre;
		this.email = email;
		this.ingresosMensuales = ingresosMensuales;
		this.deudaMensual = deudaMensual;
		this.antiguedadLaboralMeses = antiguedadLaboralMeses;
		this.creadoEn = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getDocumento() {
		return documento;
	}

	public String getNombre() {
		return nombre;
	}

	public String getEmail() {
		return email;
	}

	public BigDecimal getIngresosMensuales() {
		return ingresosMensuales;
	}

	public BigDecimal getDeudaMensual() {
		return deudaMensual;
	}

	public int getAntiguedadLaboralMeses() {
		return antiguedadLaboralMeses;
	}

}
