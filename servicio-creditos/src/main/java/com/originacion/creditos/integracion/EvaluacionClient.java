package com.originacion.creditos.integracion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EvaluacionClient {

	public record Peticion(UUID solicitudId, BigDecimal ingresosMensuales, BigDecimal deudaMensual,
			int antiguedadLaboralMeses, BigDecimal monto, BigDecimal cuotaEstimada) {
	}

	public record Factor(String nombre, int puntos, int maximo, String detalle) {
	}

	public record Resultado(int score, BigDecimal dti, String decision, BigDecimal tasaAnual, String motivo,
			List<Factor> factores) {
	}

	private final RestClient http;

	private final CircuitBreakerFactory<?, ?> circuitos;

	EvaluacionClient(RestClient evaluacionRestClient, CircuitBreakerFactory<?, ?> circuitos) {
		this.http = evaluacionRestClient;
		this.circuitos = circuitos;
	}

	public Resultado evaluar(Peticion peticion) {
		String correlationId = MDC.get("correlationId");
		return circuitos.create("evaluacion").run(() -> http.post()
			.uri("/evaluaciones")
			.header("X-Correlation-Id", correlationId)
			.body(peticion)
			.retrieve()
			.body(Resultado.class));
	}

}
