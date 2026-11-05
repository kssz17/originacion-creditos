package com.originacion.clientes;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
class ClienteController {

	record NuevoCliente(@NotBlank String documento, @NotBlank String nombre, @Email @NotBlank String email,
			@NotNull @DecimalMin("0.01") BigDecimal ingresosMensuales, @NotNull @DecimalMin("0.00") BigDecimal deudaMensual,
			@Min(0) int antiguedadLaboralMeses) {
	}

	record ClienteDto(UUID id, String documento, String nombre, String email, BigDecimal ingresosMensuales,
			BigDecimal deudaMensual, int antiguedadLaboralMeses) {

		static ClienteDto de(Cliente c) {
			return new ClienteDto(c.getId(), c.getDocumento(), c.getNombre(), c.getEmail(), c.getIngresosMensuales(),
					c.getDeudaMensual(), c.getAntiguedadLaboralMeses());
		}

	}

	private final ClienteRepository clientes;

	ClienteController(ClienteRepository clientes) {
		this.clientes = clientes;
	}

	@PostMapping("/clientes")
	@ResponseStatus(HttpStatus.CREATED)
	ClienteDto crear(@RequestHeader("X-Rol") String rol, @Valid @RequestBody NuevoCliente nuevo) {
		if (!"ANALISTA".equals(rol)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		if (clientes.existsByDocumento(nuevo.documento())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El documento ya está registrado");
		}
		return ClienteDto.de(clientes.save(new Cliente(nuevo.documento(), nuevo.nombre(), nuevo.email(),
				nuevo.ingresosMensuales(), nuevo.deudaMensual(), nuevo.antiguedadLaboralMeses())));
	}

	@GetMapping("/clientes/{id}")
	ClienteDto consultar(@PathVariable UUID id, @RequestHeader("X-Rol") String rol,
			@RequestHeader(name = "X-Cliente-Id", required = false) UUID clienteActual) {
		if (!"ANALISTA".equals(rol) && !id.equals(clienteActual)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return buscar(id);
	}

	// El gateway solo enruta /clientes/**: esta ruta es alcanzable únicamente desde la red interna.
	@GetMapping("/interno/clientes/{id}")
	ClienteDto consultarInterno(@PathVariable UUID id) {
		return buscar(id);
	}

	private ClienteDto buscar(UUID id) {
		return clientes.findById(id).map(ClienteDto::de).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

}
