package com.originacion.identidad;

import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** Entrada sin clave para quien visita la demo. Solo existe con el perfil demo y solo para usuarios ficticios. */
@RestController
@RequestMapping("/auth")
@Profile("demo")
class AccesoDemoController {

	private static final Set<String> USUARIOS_DEMO = Set.of("ana", "luis", "carla", "maria", "jorge");

	record AccesoDemo(@NotBlank String usuario) {
	}

	private final UsuarioRepository usuarios;

	private final EmisorToken emisor;

	AccesoDemoController(UsuarioRepository usuarios, EmisorToken emisor) {
		this.usuarios = usuarios;
		this.emisor = emisor;
	}

	@PostMapping("/demo")
	EmisorToken.LoginResponse entrar(@Valid @RequestBody AccesoDemo acceso) {
		if (!USUARIOS_DEMO.contains(acceso.usuario())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		return usuarios.findByUsuario(acceso.usuario())
			.map(emisor::emitir)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

}
