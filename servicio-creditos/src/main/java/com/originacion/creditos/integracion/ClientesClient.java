package com.originacion.creditos.integracion;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class ClientesClient {

	public record DatosCliente(BigDecimal ingresosMensuales, BigDecimal deudaMensual, int antiguedadLaboralMeses) {
	}

	private final RestClient http;

	private final CircuitBreakerFactory<?, ?> circuitos;

	ClientesClient(RestClient clientesRestClient, CircuitBreakerFactory<?, ?> circuitos) {
		this.http = clientesRestClient;
		this.circuitos = circuitos;
	}

	public Optional<DatosCliente> obtener(UUID clienteId) {
		// El circuit breaker ejecuta en otro hilo: el MDC debe leerse antes de entrar.
		String correlationId = MDC.get("correlationId");
		return circuitos.create("clientes").run(() -> {
			try {
				return Optional.ofNullable(http.get()
					.uri("/interno/clientes/{id}", clienteId)
					.header("X-Correlation-Id", correlationId)
					.retrieve()
					.body(DatosCliente.class));
			}
			catch (HttpClientErrorException.NotFound noExiste) {
				return Optional.empty();
			}
		});
	}

}
