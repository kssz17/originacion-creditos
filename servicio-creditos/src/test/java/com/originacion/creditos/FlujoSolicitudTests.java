package com.originacion.creditos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.originacion.creditos.aplicacion.OrquestadorSolicitudes;
import com.originacion.creditos.integracion.ClientesClient;
import com.originacion.creditos.integracion.EvaluacionClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "creditos.reintento-ms=3600000")
@AutoConfigureMockMvc
class FlujoSolicitudTests {

	private static final String CUERPO = "{\"monto\":10000.00,\"plazoMeses\":12}";

	private static final EvaluacionClient.Resultado APROBADA = new EvaluacionClient.Resultado(900,
			new BigDecimal("0.1000"), "APROBADA", new BigDecimal("0.1400"), "Dentro de política",
			List.of(new EvaluacionClient.Factor("Endeudamiento", 250, 250, "10 % de tus ingresos")));

	private static final EvaluacionClient.Resultado EN_REVISION = new EvaluacionClient.Resultado(650,
			new BigDecimal("0.3000"), "REVISION_MANUAL", new BigDecimal("0.2000"), "Requiere analista", List.of());

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	OrquestadorSolicitudes orquestador;

	@MockitoBean
	ClientesClient clientes;

	@MockitoBean
	EvaluacionClient evaluacion;

	UUID clienteId = UUID.randomUUID();

	@BeforeEach
	void clienteExistente() {
		given(clientes.obtener(any())).willReturn(Optional.of(
				new ClientesClient.DatosCliente(new BigDecimal("6000"), new BigDecimal("100"), 48)));
	}

	@Test
	void aprobacionAutomaticaGeneraElPlanYDejaAuditoria() throws Exception {
		given(evaluacion.evaluar(any())).willReturn(APROBADA);

		String id = crear("clave-1").andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("APROBADA"))
			.andReturn().getResponse().getContentAsString();

		mvc.perform(comoAnalista(get("/solicitudes/{id}", (String) JsonPath.read(id, "$.id")), "maria"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cuotas.length()").value(12))
			.andExpect(jsonPath("$.cuotas[11].saldo").value(0.0))
			.andExpect(jsonPath("$.auditoria.length()").value(2))
			.andExpect(jsonPath("$.auditoria[1].estadoNuevo").value("APROBADA"));
	}

	@Test
	void repetirLaPeticionNoDuplicaLaSolicitud() throws Exception {
		given(evaluacion.evaluar(any())).willReturn(APROBADA);

		String primera = crear("clave-2").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

		crear("clave-2").andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value((String) JsonPath.read(primera, "$.id")));
		assertThat(contarSolicitudes()).isEqualTo(1);
	}

	@Test
	void reutilizarLaClaveConOtrosDatosEsUnConflicto() throws Exception {
		given(evaluacion.evaluar(any())).willReturn(APROBADA);
		crear("clave-3").andExpect(status().isCreated());

		mvc.perform(comoCliente(post("/solicitudes")).header("Idempotency-Key", "clave-3")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"monto\":20000.00,\"plazoMeses\":12}"))
			.andExpect(status().isConflict());
	}

	@Test
	void unClienteNoVeSolicitudesAjenas() throws Exception {
		given(evaluacion.evaluar(any())).willReturn(APROBADA);
		String id = idDe(crear("clave-4"));

		mvc.perform(get("/solicitudes/{id}", id).header("X-Usuario", "otro").header("X-Rol", "CLIENTE")
			.header("X-Cliente-Id", UUID.randomUUID()))
			.andExpect(status().isNotFound());
	}

	@Test
	void quienApruebaNoPuedeDesembolsar() throws Exception {
		given(evaluacion.evaluar(any())).willReturn(EN_REVISION);
		String id = idDe(crear("clave-5"));

		mvc.perform(comoAnalista(post("/solicitudes/{id}/decision", id), "maria")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"aprobada\":true,\"motivo\":\"Ingresos verificados\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("APROBADA"));

		mvc.perform(comoAnalista(post("/solicitudes/{id}/desembolso", id), "maria")).andExpect(status().isConflict());

		mvc.perform(comoAnalista(post("/solicitudes/{id}/desembolso", id), "jorge"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("DESEMBOLSADA"));
	}

	@Test
	void unaTransicionInvalidaSeRechaza() throws Exception {
		given(evaluacion.evaluar(any())).willReturn(EN_REVISION);
		String id = idDe(crear("clave-6"));

		mvc.perform(comoAnalista(post("/solicitudes/{id}/desembolso", id), "jorge")).andExpect(status().isConflict());
	}

	@Test
	void unFalloTemporalSeRecuperaConElReintento() throws Exception {
		given(evaluacion.evaluar(any())).willThrow(new IllegalStateException("Servicio caído"));
		String id = idDe(crear("clave-7").andExpect(jsonPath("$.estado").value("RECIBIDA")));

		org.mockito.Mockito.reset(evaluacion);
		given(evaluacion.evaluar(any())).willReturn(APROBADA);
		orquestador.procesar(UUID.fromString(id));

		mvc.perform(comoAnalista(get("/solicitudes/{id}", id), "maria"))
			.andExpect(jsonPath("$.solicitud.estado").value("APROBADA"));
	}

	@Test
	void laAuditoriaNoSePuedeModificar() throws Exception {
		given(evaluacion.evaluar(any())).willReturn(APROBADA);
		crear("clave-8");

		assertThatThrownBy(() -> jdbc.update("UPDATE auditoria SET actor = 'alterado'"))
			.isInstanceOf(DataAccessException.class)
			.hasMessageContaining("solo inserción");
	}

	@Test
	void lasOfertasCalculanLaCuotaPorPlazo() throws Exception {
		mvc.perform(comoCliente(get("/solicitudes/ofertas")).param("monto", "10000"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(7))
			.andExpect(jsonPath("$[0].plazoMeses").value(12))
			.andExpect(jsonPath("$[0].cuotaDesde").value(897.87));

		mvc.perform(comoCliente(get("/solicitudes/ofertas")).param("monto", "10")).andExpect(status().isBadRequest());
	}

	@Test
	void elResumenYLosMovimientosSeLimitanAlCliente() throws Exception {
		given(evaluacion.evaluar(any())).willReturn(APROBADA);
		String id = idDe(crear("clave-9"));
		mvc.perform(comoAnalista(post("/solicitudes/{id}/desembolso", id), "jorge")).andExpect(status().isOk());

		mvc.perform(comoCliente(get("/solicitudes/resumen")))
			.andExpect(jsonPath("$.porEstado.DESEMBOLSADA").value(1))
			.andExpect(jsonPath("$.montoDesembolsado").value(10000.0))
			.andExpect(jsonPath("$.ultimoDti").value(0.1));

		mvc.perform(comoCliente(get("/solicitudes/movimientos")))
			.andExpect(jsonPath("$.length()").value(3))
			.andExpect(jsonPath("$[0].estado").value("DESEMBOLSADA"))
			.andExpect(jsonPath("$[0].actor").doesNotExist());

		mvc.perform(get("/solicitudes/movimientos").header("X-Usuario", "otro").header("X-Rol", "CLIENTE")
			.header("X-Cliente-Id", UUID.randomUUID()))
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void elDetalleExplicaElScore() throws Exception {
		given(evaluacion.evaluar(any())).willReturn(APROBADA);
		String id = idDe(crear("clave-10"));

		mvc.perform(comoCliente(get("/solicitudes/{id}", id)))
			.andExpect(jsonPath("$.factores[0].nombre").value("Endeudamiento"))
			.andExpect(jsonPath("$.factores[0].puntos").value(250));
	}

	@Test
	void pagarUnaCuotaReduceElSaldoYNoSeCobraDosVeces() throws Exception {
		String id = creditoDesembolsado("clave-11");

		pagar(id, "pago-1", "CUOTA").andExpect(status().isCreated()).andExpect(jsonPath("$.recargo").value(0.0));
		pagar(id, "pago-1", "CUOTA").andExpect(status().isOk());
		pagar(id, "pago-1", "LIQUIDACION").andExpect(status().isConflict());

		mvc.perform(comoCliente(get("/solicitudes/{id}", id)))
			.andExpect(jsonPath("$.cuotas[0].estado").value("PAGADA"))
			.andExpect(jsonPath("$.cuotas[1].estado").value("PENDIENTE"))
			.andExpect(jsonPath("$.pagos.length()").value(1));
		assertThat(contarPagos(id)).isEqualTo(1);
	}

	@Test
	void unaCuotaVencidaLlevaRecargoYSeVeEnLaAnalitica() throws Exception {
		String id = creditoDesembolsado("clave-12");
		jdbc.update("UPDATE cuotas SET vencimiento = current_date - 10 WHERE solicitud_id = ?::uuid AND numero = 1", id);

		mvc.perform(comoAnalista(get("/solicitudes/analitica"), "maria"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.creditosEnMora").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
			.andExpect(jsonPath("$.desembolsadoPorMes.length()").value(6));

		pagar(id, "pago-mora", "CUOTA").andExpect(status().isCreated())
			.andExpect(jsonPath("$.recargo").value(org.hamcrest.Matchers.greaterThan(0.0)));
	}

	@Test
	void liquidarCierraElCreditoSinCobrarInteresesFuturos() throws Exception {
		String id = creditoDesembolsado("clave-13");

		pagar(id, "liquidacion", "LIQUIDACION").andExpect(status().isCreated())
			.andExpect(jsonPath("$.capital").value(10000.0))
			.andExpect(jsonPath("$.interes").value(0.0))
			.andExpect(jsonPath("$.saldoRestante").value(0.0));

		mvc.perform(comoCliente(get("/solicitudes/{id}", id)))
			.andExpect(jsonPath("$.solicitud.estado").value("LIQUIDADA"))
			.andExpect(jsonPath("$.saldoPendiente").value(0));
		pagar(id, "tarde", "CUOTA").andExpect(status().isConflict());
	}

	@Test
	void pagosSimultaneosCobranCuotasDistintas() throws Exception {
		String id = creditoDesembolsado("clave-14");
		Callable<Integer> primero = () -> pagar(id, "hilo-1", "CUOTA").andReturn().getResponse().getStatus();
		Callable<Integer> segundo = () -> pagar(id, "hilo-2", "CUOTA").andReturn().getResponse().getStatus();

		try (var hilos = Executors.newFixedThreadPool(2)) {
			var resultados = hilos.invokeAll(List.of(primero, segundo));
			assertThat(resultados.get(0).get()).isEqualTo(201);
			assertThat(resultados.get(1).get()).isEqualTo(201);
		}

		assertThat(jdbc.queryForObject("SELECT count(*) FROM cuotas WHERE solicitud_id = ?::uuid AND pagada_en IS NOT NULL",
				Integer.class, id)).isEqualTo(2);
	}

	@Test
	void soloElTitularPuedePagar() throws Exception {
		String id = creditoDesembolsado("clave-15");

		mvc.perform(post("/solicitudes/{id}/pagos", id).header("X-Usuario", "otro").header("X-Rol", "CLIENTE")
			.header("X-Cliente-Id", UUID.randomUUID()).header("Idempotency-Key", "x")
			.contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"CUOTA\"}"))
			.andExpect(status().isNotFound());
		mvc.perform(comoAnalista(post("/solicitudes/{id}/pagos", id), "maria").header("Idempotency-Key", "x")
			.contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"CUOTA\"}"))
			.andExpect(status().isForbidden());
		mvc.perform(comoCliente(get("/solicitudes/analitica"))).andExpect(status().isForbidden());
	}

	private String creditoDesembolsado(String clave) throws Exception {
		given(evaluacion.evaluar(any())).willReturn(APROBADA);
		String id = idDe(crear(clave));
		mvc.perform(comoAnalista(post("/solicitudes/{id}/desembolso", id), "jorge")).andExpect(status().isOk());
		return id;
	}

	private ResultActions pagar(String id, String clave, String tipo) throws Exception {
		return mvc.perform(comoCliente(post("/solicitudes/{id}/pagos", id)).header("Idempotency-Key", clave)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"tipo\":\"%s\"}".formatted(tipo)));
	}

	private Integer contarPagos(String id) {
		return jdbc.queryForObject("SELECT count(*) FROM pagos WHERE solicitud_id = ?::uuid", Integer.class, id);
	}

	private ResultActions crear(String clave) throws Exception {
		return mvc.perform(comoCliente(post("/solicitudes")).header("Idempotency-Key", clave)
			.contentType(MediaType.APPLICATION_JSON)
			.content(CUERPO));
	}

	private String idDe(ResultActions resultado) throws Exception {
		return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private MockHttpServletRequestBuilder comoCliente(MockHttpServletRequestBuilder peticion) {
		return peticion.header("X-Usuario", "ana").header("X-Rol", "CLIENTE").header("X-Cliente-Id", clienteId);
	}

	private MockHttpServletRequestBuilder comoAnalista(MockHttpServletRequestBuilder peticion, String usuario) {
		return peticion.header("X-Usuario", usuario).header("X-Rol", "ANALISTA");
	}

	private Integer contarSolicitudes() {
		return jdbc.queryForObject("SELECT count(*) FROM solicitudes WHERE cliente_id = ?", Integer.class, clienteId);
	}

}
