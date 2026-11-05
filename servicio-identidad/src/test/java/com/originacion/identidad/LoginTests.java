package com.originacion.identidad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class LoginTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder encoder;

	@BeforeEach
	void crearUsuario() {
		usuarios.deleteAll();
		usuarios.save(new Usuario("ana", "Ana", "ana@test.dev", encoder.encode("clave-correcta"), "CLIENTE", UUID.randomUUID()));
	}

	@Test
	void credencialesValidasDevuelvenToken() throws Exception {
		mvc.perform(login("ana", "clave-correcta"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andExpect(jsonPath("$.rol").value("CLIENTE"));
	}

	@Test
	void claveIncorrectaYUsuarioInexistenteResponden401() throws Exception {
		mvc.perform(login("ana", "otra")).andExpect(status().isUnauthorized());
		mvc.perform(login("nadie", "otra")).andExpect(status().isUnauthorized());
	}

	@Test
	void elAccesoDemoNoExisteFueraDelPerfilDemo() throws Exception {
		mvc.perform(post("/auth/demo").contentType(MediaType.APPLICATION_JSON).content("{\"usuario\":\"ana\"}"))
			.andExpect(status().isNotFound());
	}

	@Test
	void elUsuarioConsultaYActualizaSuPerfil() throws Exception {
		mvc.perform(get("/auth/perfil").header("X-Usuario", "ana"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value("Ana"));

		mvc.perform(put("/auth/perfil").header("X-Usuario", "ana").contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"Ana Pérez\",\"email\":\"ana@nuevo.dev\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("ana@nuevo.dev"));
	}

	@Test
	void cambiarLaClaveExigeLaActual() throws Exception {
		mvc.perform(cambioClave("incorrecta", "clave-nueva-1")).andExpect(status().isBadRequest());

		mvc.perform(cambioClave("clave-correcta", "clave-nueva-1")).andExpect(status().isNoContent());

		mvc.perform(login("ana", "clave-nueva-1")).andExpect(status().isOk());
		mvc.perform(login("ana", "clave-correcta")).andExpect(status().isUnauthorized());
	}

	private MockHttpServletRequestBuilder cambioClave(String actual, String nueva) {
		return post("/auth/clave").header("X-Usuario", "ana").contentType(MediaType.APPLICATION_JSON)
			.content("{\"actual\":\"%s\",\"nueva\":\"%s\"}".formatted(actual, nueva));
	}

	private MockHttpServletRequestBuilder login(String usuario, String clave) {
		return post("/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"usuario\":\"%s\",\"clave\":\"%s\"}".formatted(usuario, clave));
	}

}
