package com.originacion.evaluacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RestController
class EvaluacionController {

	record PeticionEvaluacion(@NotNull UUID solicitudId, @NotNull @DecimalMin("0.01") BigDecimal ingresosMensuales,
			@NotNull @DecimalMin("0.00") BigDecimal deudaMensual, @Min(0) int antiguedadLaboralMeses,
			@NotNull @DecimalMin("0.01") BigDecimal monto, @NotNull @DecimalMin("0.01") BigDecimal cuotaEstimada) {
	}

	record EvaluacionDto(UUID solicitudId, int score, BigDecimal dti, Decision decision, BigDecimal tasaAnual,
			String motivo, List<Factor> factores) {

		static EvaluacionDto de(Evaluacion e) {
			return new EvaluacionDto(e.getSolicitudId(), e.getScore(), e.getDti(), e.getDecision(), e.getTasaAnual(),
					e.getMotivo(), List.copyOf(e.getFactores()));
		}

	}

	private final EvaluacionRepository evaluaciones;

	EvaluacionController(EvaluacionRepository evaluaciones) {
		this.evaluaciones = evaluaciones;
	}

	/** Idempotente por solicitud: un reintento devuelve la decisión original en lugar de recalcularla. */
	@PostMapping("/evaluaciones")
	EvaluacionDto evaluar(@Valid @RequestBody PeticionEvaluacion peticion) {
		return evaluaciones.findBySolicitudId(peticion.solicitudId()).map(EvaluacionDto::de).orElseGet(() -> {
			var resultado = MotorScoring.evaluar(new MotorScoring.Datos(peticion.ingresosMensuales(),
					peticion.deudaMensual(), peticion.antiguedadLaboralMeses(), peticion.monto(), peticion.cuotaEstimada()));
			try {
				return EvaluacionDto.de(evaluaciones.saveAndFlush(new Evaluacion(peticion.solicitudId(), resultado)));
			}
			catch (DataIntegrityViolationException carrera) {
				return EvaluacionDto.de(evaluaciones.findBySolicitudId(peticion.solicitudId()).orElseThrow());
			}
		});
	}

}
