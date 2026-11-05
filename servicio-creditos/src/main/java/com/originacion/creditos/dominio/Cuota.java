package com.originacion.creditos.dominio;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "cuotas")
@IdClass(Cuota.Clave.class)
public class Cuota {

	private static final BigDecimal RECARGO_MORA = new BigDecimal("0.02");

	public record Clave(UUID solicitudId, int numero) implements Serializable {

		public Clave() {
			this(null, 0);
		}

	}

	@Id
	@Column(name = "solicitud_id")
	private UUID solicitudId;

	@Id
	private int numero;

	@Column(nullable = false)
	private BigDecimal capital;

	@Column(nullable = false)
	private BigDecimal interes;

	@Column(nullable = false)
	private BigDecimal total;

	@Column(nullable = false)
	private BigDecimal saldo;

	private LocalDate vencimiento;

	@Column(name = "pagada_en")
	private Instant pagadaEn;

	protected Cuota() {
	}

	public Cuota(UUID solicitudId, PlanAmortizacion.Fila fila) {
		this.solicitudId = solicitudId;
		this.numero = fila.numero();
		this.capital = fila.capital();
		this.interes = fila.interes();
		this.total = fila.total();
		this.saldo = fila.saldo();
	}

	public void fijarVencimiento(LocalDate vencimiento) {
		this.vencimiento = vencimiento;
	}

	public void marcarPagada(Instant momento) {
		this.pagadaEn = momento;
	}

	public boolean estaVencida(LocalDate hoy) {
		return pagadaEn == null && vencimiento != null && vencimiento.isBefore(hoy);
	}

	/** Recargo fijo por retraso: ilustrativo, sin intereses de demora acumulados por día. */
	public BigDecimal recargo(LocalDate hoy) {
		return estaVencida(hoy) ? total.multiply(RECARGO_MORA).setScale(2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO.setScale(2);
	}

	public int getNumero() {
		return numero;
	}

	public BigDecimal getCapital() {
		return capital;
	}

	public BigDecimal getInteres() {
		return interes;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public BigDecimal getSaldo() {
		return saldo;
	}

	public LocalDate getVencimiento() {
		return vencimiento;
	}

	public Instant getPagadaEn() {
		return pagadaEn;
	}

}
