package com.originacion.identidad;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/auth")
class AuthController {

	record LoginRequest(@NotBlank String usuario, @NotBlank String clave) {
	}

	private final UsuarioRepository usuarios;

	private final PasswordEncoder encoder;

	private final EmisorToken emisor;

	// Se compara contra este hash si el usuario no existe, para no revelarlo por el tiempo de respuesta.
	private final String hashSenuelo;

	AuthController(UsuarioRepository usuarios, PasswordEncoder encoder, EmisorToken emisor) {
		this.usuarios = usuarios;
		this.encoder = encoder;
		this.emisor = emisor;
		this.hashSenuelo = encoder.encode(UUID.randomUUID().toString());
	}

	@PostMapping("/login")
	EmisorToken.LoginResponse login(@Valid @RequestBody LoginRequest peticion) {
		Optional<Usuario> usuario = usuarios.findByUsuario(peticion.usuario());
		boolean claveCorrecta = encoder.matches(peticion.clave(), usuario.map(Usuario::getClaveHash).orElse(hashSenuelo));
		if (usuario.isEmpty() || !claveCorrecta) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
		}
		return emisor.emitir(usuario.get());
	}

}
