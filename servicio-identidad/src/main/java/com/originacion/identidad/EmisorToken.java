package com.originacion.identidad;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
class EmisorToken {

	record LoginResponse(String token, Instant expiraEn, String usuario, String rol, UUID clienteId) {
	}

	private final JwtEncoder jwtEncoder;

	private final Duration duracion;

	EmisorToken(JwtEncoder jwtEncoder, @Value("${jwt.duracion}") Duration duracion) {
		this.jwtEncoder = jwtEncoder;
		this.duracion = duracion;
	}

	LoginResponse emitir(Usuario usuario) {
		Instant ahora = Instant.now();
		Instant expira = ahora.plus(duracion);
		var claims = JwtClaimsSet.builder()
			.subject(usuario.getUsuario())
			.issuedAt(ahora)
			.expiresAt(expira)
			.claim("rol", usuario.getRol());
		if (usuario.getClienteId() != null) {
			claims.claim("cliente_id", usuario.getClienteId().toString());
		}
		var parametros = JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build());
		String token = jwtEncoder.encode(parametros).getTokenValue();
		return new LoginResponse(token, expira, usuario.getUsuario(), usuario.getRol(), usuario.getClienteId());
	}

}
