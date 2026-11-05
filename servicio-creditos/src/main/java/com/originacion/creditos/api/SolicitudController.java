package com.originacion.creditos.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.originacion.creditos.api.Respuestas.CuotaDto;
import com.originacion.creditos.api.Respuestas.DetalleDto;
import com.originacion.creditos.api.Respuestas.EventoDto;
import com.originacion.creditos.api.Respuestas.FactorDto;
import com.originacion.creditos.api.Respuestas.MovimientoDto;
import com.originacion.creditos.api.Respuestas.OfertaDto;
import com.originacion.creditos.api.Respuestas.PagoDto;
import com.originacion.creditos.api.Respuestas.ResumenDto;
import com.originacion.creditos.api.Respuestas.SolicitudDto;
import com.originacion.creditos.aplicacion.Actor;
import com.originacion.creditos.aplicacion.OrquestadorSolicitudes;
import com.originacion.creditos.aplicacion.SolicitudNoEncontradaException;
import com.originacion.creditos.aplicacion.SolicitudService;
import com.originacion.creditos.dominio.AuditoriaRepository;
import com.originacion.creditos.dominio.Cuota;
import com.originacion.creditos.dominio.CuotaRepository;
import com.originacion.creditos.dominio.EstadoSolicitud;
import com.originacion.creditos.dominio.FactorScoreRepository;
import com.originacion.creditos.dominio.PagoRepository;
import com.originacion.creditos.dominio.PlanAmortizacion;
import com.originacion.creditos.dominio.ResumenEstado;
import com.originacion.creditos.dominio.Solicitud;
import com.originacion.creditos.dominio.SolicitudRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/solicitudes")
class SolicitudController {

	private static final List<Integer> PLAZOS = List.of(12, 24, 36, 48, 60, 72, 84);

	// Extremos de los tramos de servicio-evaluacion: la tasa final depende del score.
	private static final BigDecimal TASA_DESDE = new BigDecimal("0.14");

	private static final BigDecimal TASA_HASTA = new BigDecimal("0.20");

	record NuevaSolicitud(
			@NotNull @DecimalMin("1000.00") @DecimalMax("100000.00") @Digits(integer = 6, fraction = 2) BigDecimal monto,
			@Min(6) @Max(84) int plazoMeses) {
	}

	record DecisionAnalista(@NotNull Boolean aprobada, @NotBlank @Size(max = 300) String motivo) {
	}

	private final OrquestadorSolicitudes orquestador;

	private final SolicitudService servicio;

	private final SolicitudRepository solicitudes;

	private final CuotaRepository cuotas;

	private final AuditoriaRepository auditoria;

	private final PagoRepository pagos;

	private final FactorScoreRepository factores;

	SolicitudController(OrquestadorSolicitudes orquestador, SolicitudService servicio, SolicitudRepository solicitudes,
			CuotaRepository cuotas, AuditoriaRepository auditoria, PagoRepository pagos, FactorScoreRepository factores) {
		this.orquestador = orquestador;
		this.servicio = servicio;
		this.solicitudes = solicitudes;
		this.cuotas = cuotas;
		this.auditoria = auditoria;
		this.pagos = pagos;
		this.factores = factores;
	}

	@PostMapping
	ResponseEntity<SolicitudDto> crear(Actor actor,
			@RequestHeader("Idempotency-Key") @NotBlank @Size(max = 64) String idempotencyKey,
			@Valid @RequestBody NuevaSolicitud nueva) {
		if (!actor.esCliente()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un cliente puede solicitar un crédito");
		}
		var creacion = orquestador.crear(actor, idempotencyKey, nueva.monto(), nueva.plazoMeses());
		return ResponseEntity.status(creacion.nueva() ? HttpStatus.CREATED : HttpStatus.OK)
			.body(SolicitudDto.de(creacion.solicitud()));
	}

	@GetMapping
	PagedModel<SolicitudDto> listar(Actor actor, @RequestParam(required = false) EstadoSolicitud estado,
			@RequestParam(defaultValue = "0") @Min(0) int pagina,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
		Pageable paginacion = PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "creadaEn"));
		return new PagedModel<>(consultar(actor, estado, paginacion).map(SolicitudDto::de));
	}

	@GetMapping("/ofertas")
	List<OfertaDto> ofertas(@RequestParam @DecimalMin("1000.00") @DecimalMax("100000.00") BigDecimal monto) {
		return PLAZOS.stream().map(plazo -> {
			BigDecimal cuotaDesde = PlanAmortizacion.cuota(monto, TASA_DESDE, plazo);
			BigDecimal cuotaHasta = PlanAmortizacion.cuota(monto, TASA_HASTA, plazo);
			BigDecimal interes = cuotaDesde.multiply(BigDecimal.valueOf(plazo)).subtract(monto);
			return new OfertaDto(plazo, TASA_DESDE, TASA_HASTA, cuotaDesde, cuotaHasta, interes);
		}).toList();
	}

	@GetMapping("/resumen")
	ResumenDto resumen(Actor actor) {
		List<ResumenEstado> filas = actor.esAnalista() ? solicitudes.resumenGlobal()
				: solicitudes.resumenDeCliente(actor.clienteId());
		Map<EstadoSolicitud, Long> porEstado = new EnumMap<>(EstadoSolicitud.class);
		BigDecimal desembolsado = BigDecimal.ZERO;
		BigDecimal cuotas = BigDecimal.ZERO;
		for (ResumenEstado fila : filas) {
			porEstado.put(fila.getEstado(), fila.getTotal());
			if (fila.getEstado() == EstadoSolicitud.DESEMBOLSADA) {
				desembolsado = fila.getMonto();
				cuotas = fila.getCuotas();
			}
		}
		BigDecimal ultimoDti = actor.esAnalista() ? null
				: solicitudes.findFirstByClienteIdAndDtiNotNullOrderByCreadaEnDesc(actor.clienteId())
					.map(Solicitud::getDti)
					.orElse(null);
		List<Cuota> pendientes = actor.esAnalista() ? List.of()
				: this.cuotas.pendientesDeCliente(actor.clienteId(), EstadoSolicitud.DESEMBOLSADA);
		LocalDate hoy = LocalDate.now();
		BigDecimal saldo = pendientes.stream().map(Cuota::getCapital).reduce(BigDecimal.ZERO, BigDecimal::add);
		long vencidas = pendientes.stream().filter(cuota -> cuota.estaVencida(hoy)).count();
		Cuota proxima = pendientes.isEmpty() ? null : pendientes.getFirst();
		return new ResumenDto(porEstado, desembolsado, cuotas, ultimoDti, saldo, vencidas,
				proxima == null ? null : proxima.getVencimiento(),
				proxima == null ? null : proxima.getTotal().add(proxima.recargo(hoy)));
	}

	@GetMapping("/movimientos")
	List<MovimientoDto> movimientos(Actor actor, @RequestParam(defaultValue = "30") @Min(1) @Max(100) int limite) {
		Pageable primeros = PageRequest.of(0, limite);
		var eventos = actor.esAnalista() ? auditoria.findAllByOrderByIdDesc(primeros)
				: auditoria.recientesDeCliente(actor.clienteId(), primeros);
		return eventos.stream().map(evento -> MovimientoDto.de(evento, actor.esAnalista(), actor.usuario())).toList();
	}

	@GetMapping("/{id}")
	DetalleDto detalle(Actor actor, @PathVariable UUID id) {
		Solicitud solicitud = solicitudes.findById(id).orElseThrow(SolicitudNoEncontradaException::new);
		if (!actor.esAnalista() && !solicitud.getClienteId().equals(actor.clienteId())) {
			// 404 y no 403: no se revela que la solicitud existe.
			throw new SolicitudNoEncontradaException();
		}
		LocalDate hoy = LocalDate.now();
		List<Cuota> plan = cuotas.findBySolicitudIdOrderByNumero(id);
		List<Cuota> pendientes = plan.stream().filter(cuota -> cuota.getPagadaEn() == null).toList();
		BigDecimal saldo = pendientes.stream().map(Cuota::getCapital).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal liquidacion = pendientes.stream()
			.filter(cuota -> cuota.estaVencida(hoy))
			.map(cuota -> cuota.getInteres().add(cuota.recargo(hoy)))
			.reduce(saldo, BigDecimal::add);
		List<EventoDto> eventos = actor.esAnalista()
				? auditoria.findBySolicitudIdOrderById(id).stream().map(EventoDto::de).toList() : List.of();
		return new DetalleDto(SolicitudDto.de(solicitud), plan.stream().map(cuota -> CuotaDto.de(cuota, hoy)).toList(),
				eventos, pagos.findBySolicitudIdOrderByCreadoEnDesc(id).stream().map(PagoDto::de).toList(),
				factores.findBySolicitudIdOrderByOrden(id).stream().map(FactorDto::de).toList(), saldo, liquidacion);
	}

	@PostMapping("/{id}/decision")
	SolicitudDto decidir(Actor actor, @PathVariable UUID id, @Valid @RequestBody DecisionAnalista decision) {
		exigirAnalista(actor);
		return SolicitudDto.de(servicio.decidir(id, actor, decision.aprobada(), decision.motivo()));
	}

	@PostMapping("/{id}/desembolso")
	SolicitudDto desembolsar(Actor actor, @PathVariable UUID id) {
		exigirAnalista(actor);
		return SolicitudDto.de(servicio.desembolsar(id, actor));
	}

	private Page<Solicitud> consultar(Actor actor, EstadoSolicitud estado, Pageable paginacion) {
		if (!actor.esAnalista()) {
			return solicitudes.findByClienteId(actor.clienteId(), paginacion);
		}
		return estado == null ? solicitudes.findAll(paginacion) : solicitudes.findByEstado(estado, paginacion);
	}

	private void exigirAnalista(Actor actor) {
		if (!actor.esAnalista()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}

}
