package com.originacion.identidad;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El usuario llega en X-Usuario: estas rutas solo son alcanzables a través del gateway, que exige JWT. */
@RestController
@RequestMapping("/auth")
class PerfilController {

	record PerfilDto(String usuario, String nombre, String email, String rol, UUID clienteId) {

		static PerfilDto de(Usuario u) {
			return new PerfilDto(u.getUsuario(), u.getNombre(), u.getEmail(), u.getRol(), u.getClienteId());
		}

	}

	record DatosPerfil(@NotBlank @Size(max = 120) String nombre, @NotBlank @Email @Size(max = 120) String email) {
	}

	record CambioClave(@NotBlank String actual, @NotBlank @Size(min = 8, max = 72) String nueva) {
	}

	private final UsuarioRepository usuarios;

	private final PasswordEncoder encoder;

	PerfilController(UsuarioRepository usuarios, PasswordEncoder encoder) {
		this.usuarios = usuarios;
		this.encoder = encoder;
	}

	@GetMapping("/perfil")
	PerfilDto consultar(@RequestHeader("X-Usuario") String usuario) {
		return PerfilDto.de(buscar(usuario));
	}

	@PutMapping("/perfil")
	PerfilDto actualizar(@RequestHeader("X-Usuario") String usuario, @Valid @RequestBody DatosPerfil datos) {
		Usuario actual = buscar(usuario);
		actual.actualizarPerfil(datos.nombre(), datos.email());
		return PerfilDto.de(usuarios.save(actual));
	}

	@PostMapping("/clave")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void cambiarClave(@RequestHeader("X-Usuario") String usuario, @Valid @RequestBody CambioClave cambio) {
		Usuario actual = buscar(usuario);
		if (!encoder.matches(cambio.actual(), actual.getClaveHash())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clave actual no es correcta");
		}
		actual.cambiarClave(encoder.encode(cambio.nueva()));
		usuarios.save(actual);
	}

	private Usuario buscar(String usuario) {
		return usuarios.findByUsuario(usuario).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

}
