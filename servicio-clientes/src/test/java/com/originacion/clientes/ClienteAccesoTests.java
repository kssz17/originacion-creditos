package com.originacion.clientes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ClienteAccesoTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	ClienteRepository clientes;

	@Test
	void unClienteSoloVeSusPropiosDatos() throws Exception {
		Cliente cliente = clientes.save(new Cliente(UUID.randomUUID().toString().substring(0, 20), "Ana", "ana@test.dev",
				new BigDecimal("3000.00"), BigDecimal.ZERO, 12));

		mvc.perform(get("/clientes/{id}", cliente.getId()).header("X-Rol", "CLIENTE")
			.header("X-Cliente-Id", cliente.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value("Ana"));

		mvc.perform(get("/clientes/{id}", cliente.getId()).header("X-Rol", "CLIENTE")
			.header("X-Cliente-Id", UUID.randomUUID()))
			.andExpect(status().isNotFound());

		mvc.perform(get("/clientes/{id}", cliente.getId()).header("X-Rol", "ANALISTA"))
			.andExpect(status().isOk());
	}

}
