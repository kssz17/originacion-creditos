package com.originacion.gateway;

import java.nio.charset.StandardCharsets;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
class SeguridadConfig {

	@Bean
	SecurityWebFilterChain seguridad(ServerHttpSecurity http) {
		return http
			.csrf(ServerHttpSecurity.CsrfSpec::disable)
			.authorizeExchange(rutas -> rutas
				.pathMatchers("/api/auth/login", "/api/auth/demo", "/actuator/health/**").permitAll()
				.anyExchange().authenticated())
			.oauth2ResourceServer(servidor -> servidor.jwt(Customizer.withDefaults()))
			.build();
	}

	@Bean
	ReactiveJwtDecoder jwtDecoder(@Value("${jwt.secreto}") String secreto) {
		var clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		return NimbusReactiveJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
	}

}
