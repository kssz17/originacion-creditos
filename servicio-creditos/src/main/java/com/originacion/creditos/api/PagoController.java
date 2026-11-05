package com.originacion.creditos.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.originacion.creditos.api.Respuestas.PagoDto;
import com.originacion.creditos.aplicacion.Actor;
import com.originacion.creditos.aplicacion.PagoService;
import com.originacion.creditos.dominio.TipoPago;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
class PagoController {

	record NuevoPago(@NotNull TipoPago tipo) {
	}

	private final PagoService servicio;

	PagoController(PagoService servicio) {
		this.servicio = servicio;
	}

	@PostMapping("/solicitudes/{id}/pagos")
	ResponseEntity<PagoDto> pagar(Actor actor, @PathVariable UUID id,
			@RequestHeader("Idempotency-Key") @NotBlank @Size(max = 64) String idempotencyKey,
			@Valid @RequestBody NuevoPago nuevo) {
		if (!actor.esCliente()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el titular puede pagar el crédito");
		}
		var resultado = servicio.pagar(id, actor, nuevo.tipo(), idempotencyKey);
		return ResponseEntity.status(resultado.nuevo() ? HttpStatus.CREATED : HttpStatus.OK)
			.body(PagoDto.de(resultado.pago()));
	}

}
