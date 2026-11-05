package com.originacion.creditos.integracion;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class HttpConfig {

	@Bean
	RestClient clientesRestClient(RestClient.Builder builder, @Value("${servicios.clientes.url}") String url) {
		return cliente(builder, url);
	}

	@Bean
	RestClient evaluacionRestClient(RestClient.Builder builder, @Value("${servicios.evaluacion.url}") String url) {
		return cliente(builder, url);
	}

	private RestClient cliente(RestClient.Builder builder, String url) {
		var ajustes = HttpClientSettings.defaults()
			.withConnectTimeout(Duration.ofSeconds(1))
			.withReadTimeout(Duration.ofSeconds(3));
		return builder.clone()
			.baseUrl(url)
			.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(ajustes))
			.build();
	}

}
