package com.originacion.creditos.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pagos")
public class Pago {

	@Id
	private UUID id;

	@Column(name = "solicitud_id", nullable = false)
	private UUID solicitudId;

	@Column(name = "idempotency_key", nullable = false)
	private String idempotencyKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoPago tipo;

	@Column(nullable = false)
	private BigDecimal capital;

	@Column(nullable = false)
	private BigDecimal interes;

	@Column(nullable = false)
	private BigDecimal recargo;

	@Column(nullable = false)
	private BigDecimal total;

	@Column(name = "saldo_restante", nullable = false)
	private BigDecimal saldoRestante;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	protected Pago() {
	}

	public Pago(UUID solicitudId, String idempotencyKey, TipoPago tipo, BigDecimal capital, BigDecimal interes,
			BigDecimal recargo, BigDecimal saldoRestante, Instant creadoEn) {
		this.id = UUID.randomUUID();
		this.solicitudId = solicitudId;
		this.idempotencyKey = idempotencyKey;
		this.tipo = tipo;
		this.capital = capital;
		this.interes = interes;
		this.recargo = recargo;
		this.total = capital.add(interes).add(recargo);
		this.saldoRestante = saldoRestante;
		this.creadoEn = creadoEn;
	}

	public UUID getId() {
		return id;
	}

	public TipoPago getTipo() {
		return tipo;
	}

	public BigDecimal getCapital() {
		return capital;
	}

	public BigDecimal getInteres() {
		return interes;
	}

	public BigDecimal getRecargo() {
		return recargo;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public BigDecimal getSaldoRestante() {
		return saldoRestante;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
