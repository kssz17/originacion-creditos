package com.originacion.creditos.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "solicitudes")
public class Solicitud {

	@Id
	private UUID id;

	@Column(name = "cliente_id", nullable = false)
	private UUID clienteId;

	@Column(name = "idempotency_key", nullable = false)
	private String idempotencyKey;

	@Column(nullable = false)
	private BigDecimal monto;

	@Column(name = "plazo_meses", nullable = false)
	private int plazoMeses;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoSolicitud estado;

	private Integer score;

	private BigDecimal dti;

	@Column(name = "tasa_anual")
	private BigDecimal tasaAnual;

	@Column(name = "cuota_mensual")
	private BigDecimal cuotaMensual;

	private String motivo;

	private String analista;

	@Column(name = "creada_en", nullable = false)
	private Instant creadaEn;

	@Column(name = "desembolsada_en")
	private LocalDate desembolsadaEn;

	@Version
	private long version;

	protected Solicitud() {
	}

	public Solicitud(UUID clienteId, String idempotencyKey, BigDecimal monto, int plazoMeses) {
		this.id = UUID.randomUUID();
		this.clienteId = clienteId;
		this.idempotencyKey = idempotencyKey;
		this.monto = monto;
		this.plazoMeses = plazoMeses;
		this.estado = EstadoSolicitud.RECIBIDA;
		this.creadaEn = Instant.now();
	}

	public void transicionar(EstadoSolicitud destino) {
		if (!estado.puedePasarA(destino)) {
			throw new ReglaNegocioException("No se puede pasar de %s a %s".formatted(estado, destino));
		}
		estado = destino;
	}

	public void registrarEvaluacion(int score, BigDecimal dti, BigDecimal tasaAnual, String motivo) {
		this.score = score;
		this.dti = dti;
		this.tasaAnual = tasaAnual;
		this.motivo = motivo;
	}

	public void registrarRevision(String analista, String motivo) {
		this.analista = analista;
		this.motivo = motivo;
	}

	public void fijarCuota(BigDecimal cuotaMensual) {
		this.cuotaMensual = cuotaMensual;
	}

	public void desembolsar(LocalDate fecha) {
		transicionar(EstadoSolicitud.DESEMBOLSADA);
		this.desembolsadaEn = fecha;
	}

	public void liquidar() {
		transicionar(EstadoSolicitud.LIQUIDADA);
	}

	public void fijarMotivo(String motivo) {
		this.motivo = motivo;
	}

	public UUID getId() {
		return id;
	}

	public UUID getClienteId() {
		return clienteId;
	}

	public BigDecimal getMonto() {
		return monto;
	}

	public int getPlazoMeses() {
		return plazoMeses;
	}

	public EstadoSolicitud getEstado() {
		return estado;
	}

	public Integer getScore() {
		return score;
	}

	public BigDecimal getDti() {
		return dti;
	}

	public BigDecimal getTasaAnual() {
		return tasaAnual;
	}

	public BigDecimal getCuotaMensual() {
		return cuotaMensual;
	}

	public String getMotivo() {
		return motivo;
	}

	public String getAnalista() {
		return analista;
	}

	public Instant getCreadaEn() {
		return creadaEn;
	}

	public LocalDate getDesembolsadaEn() {
		return desembolsadaEn;
	}

}
