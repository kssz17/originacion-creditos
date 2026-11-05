package com.originacion.identidad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "DEMO_CLAVE=clave-de-pruebas")
@ActiveProfiles("demo")
@AutoConfigureMockMvc
class AccesoDemoTests {

	@Autowired
	MockMvc mvc;

	@Test
	void entraConUnUsuarioDemoYRechazaCualquierOtro() throws Exception {
		mvc.perform(post("/auth/demo").contentType(MediaType.APPLICATION_JSON).content("{\"usuario\":\"maria\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rol").value("ANALISTA"));

		mvc.perform(post("/auth/demo").contentType(MediaType.APPLICATION_JSON).content("{\"usuario\":\"admin\"}"))
			.andExpect(status().isUnauthorized());
	}

}
