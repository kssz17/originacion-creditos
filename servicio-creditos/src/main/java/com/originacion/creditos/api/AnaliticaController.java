package com.originacion.creditos.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.originacion.creditos.api.Respuestas.AnaliticaDto;
import com.originacion.creditos.api.Respuestas.MesDto;
import com.originacion.creditos.aplicacion.Actor;
import com.originacion.creditos.dominio.DesembolsoMes;
import com.originacion.creditos.dominio.EstadoSolicitud;
import com.originacion.creditos.dominio.ResumenEstado;
import com.originacion.creditos.dominio.SaludCartera;
import com.originacion.creditos.dominio.SolicitudRepository;

@RestController
class AnaliticaController {

	private static final int MESES = 6;

	private final SolicitudRepository solicitudes;

	AnaliticaController(SolicitudRepository solicitudes) {
		this.solicitudes = solicitudes;
	}

	@GetMapping("/solicitudes/analitica")
	AnaliticaDto analitica(Actor actor) {
		if (!actor.esAnalista()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		Map<EstadoSolicitud, Long> porEstado = new EnumMap<>(EstadoSolicitud.class);
		for (ResumenEstado fila : solicitudes.resumenGlobal()) {
			porEstado.put(fila.getEstado(), fila.getTotal());
		}
		long total = porEstado.values().stream().mapToLong(Long::longValue).sum();
		long desembolsadas = contar(porEstado, EstadoSolicitud.DESEMBOLSADA, EstadoSolicitud.LIQUIDADA);
		long aprobadas = desembolsadas + contar(porEstado, EstadoSolicitud.APROBADA);
		long rechazadas = contar(porEstado, EstadoSolicitud.RECHAZADA);
		SaludCartera salud = solicitudes.saludCartera(LocalDate.now());
		return new AnaliticaDto(total, total - contar(porEstado, EstadoSolicitud.RECIBIDA), aprobadas, desembolsadas,
				proporcion(BigDecimal.valueOf(aprobadas), BigDecimal.valueOf(aprobadas + rechazadas)), ultimosMeses(),
				salud.getSaldoActivo(), salud.getSaldoEnMora(), salud.getCreditosEnMora(),
				proporcion(salud.getSaldoEnMora(), salud.getSaldoActivo()));
	}

	private List<MesDto> ultimosMeses() {
		YearMonth actual = YearMonth.now();
		YearMonth inicio = actual.minusMonths(MESES - 1L);
		Map<String, DesembolsoMes> filas = solicitudes.desembolsadoPorMes(inicio.atDay(1)).stream()
			.collect(Collectors.toMap(DesembolsoMes::getMes, Function.identity()));
		List<MesDto> meses = new ArrayList<>(MESES);
		for (YearMonth mes = inicio; !mes.isAfter(actual); mes = mes.plusMonths(1)) {
			DesembolsoMes fila = filas.get(mes.toString());
			meses.add(fila == null ? new MesDto(mes.toString(), BigDecimal.ZERO, 0)
					: new MesDto(mes.toString(), fila.getMonto(), fila.getCreditos()));
		}
		return meses;
	}

	private long contar(Map<EstadoSolicitud, Long> porEstado, EstadoSolicitud... estados) {
		long suma = 0;
		for (EstadoSolicitud estado : estados) {
			suma += porEstado.getOrDefault(estado, 0L);
		}
		return suma;
	}

	private BigDecimal proporcion(BigDecimal parte, BigDecimal total) {
		return total.signum() == 0 ? BigDecimal.ZERO : parte.divide(total, 4, RoundingMode.HALF_UP);
	}

}
