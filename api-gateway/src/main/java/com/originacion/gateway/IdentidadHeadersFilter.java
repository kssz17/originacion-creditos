package com.originacion.gateway;

import java.util.Optional;
import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Traduce el JWT a cabeceras internas. Las cabeceras entrantes se eliminan siempre para que un
 * cliente externo no pueda hacerse pasar por otro usuario.
 */
@Component
class IdentidadHeadersFilter implements GlobalFilter, Ordered {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		return ReactiveSecurityContextHolder.getContext()
			.map(SecurityContext::getAuthentication)
			.map(Optional::of)
			.defaultIfEmpty(Optional.empty())
			.flatMap(autenticacion -> chain.filter(conCabeceras(exchange, autenticacion)));
	}

	private ServerWebExchange conCabeceras(ServerWebExchange exchange, Optional<Authentication> autenticacion) {
		var peticion = exchange.getRequest().mutate().headers(cabeceras -> {
			cabeceras.remove("X-Usuario");
			cabeceras.remove("X-Rol");
			cabeceras.remove("X-Cliente-Id");
			cabeceras.set("X-Correlation-Id", UUID.randomUUID().toString());
			autenticacion.filter(JwtAuthenticationToken.class::isInstance)
				.map(token -> ((JwtAuthenticationToken) token).getToken())
				.ifPresent(jwt -> copiarIdentidad(jwt, cabeceras));
		}).build();
		return exchange.mutate().request(peticion).build();
	}

	private void copiarIdentidad(Jwt jwt, HttpHeaders cabeceras) {
		cabeceras.set("X-Usuario", jwt.getSubject());
		cabeceras.set("X-Rol", jwt.getClaimAsString("rol"));
		String clienteId = jwt.getClaimAsString("cliente_id");
		if (clienteId != null) {
			cabeceras.set("X-Cliente-Id", clienteId);
		}
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
