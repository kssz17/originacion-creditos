package com.originacion.evaluacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MotorScoringTests {

	@ParameterizedTest
	@CsvSource({
		"6000, 200, 60, 10000, 372, APROBADA,        900, 0.1400",
		"3000, 300, 12, 10000, 372, APROBADA,        730, 0.1600",
		"2500, 400,  8, 12000, 446, REVISION_MANUAL, 650, 0.2000",
		"1800, 900, 24,  8000, 298, RECHAZADA,       580,",
		"2000, 800,  3, 30000, 200, RECHAZADA,       450,",
	})
	void aplicaLaPolitica(BigDecimal ingresos, BigDecimal deuda, int antiguedad, BigDecimal monto, BigDecimal cuota,
			Decision decision, int score, BigDecimal tasa) {
		var resultado = MotorScoring.evaluar(new MotorScoring.Datos(ingresos, deuda, antiguedad, monto, cuota));

		assertThat(resultado.decision()).isEqualTo(decision);
		assertThat(resultado.score()).isEqualTo(score);
		assertThat(resultado.tasaAnual()).isEqualTo(tasa);
		assertThat(resultado.factores()).hasSize(4);
		assertThat(resultado.factores().stream().mapToInt(Factor::puntos).sum()).isEqualTo(score);
	}

}
