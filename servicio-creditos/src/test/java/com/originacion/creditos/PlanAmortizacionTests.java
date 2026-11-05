package com.originacion.creditos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.originacion.creditos.dominio.PlanAmortizacion;

class PlanAmortizacionTests {

	@Test
	void calculaLaCuotaFrancesa() {
		assertThat(PlanAmortizacion.cuota(new BigDecimal("10000.00"), new BigDecimal("0.12"), 12))
			.isEqualByComparingTo("888.49");
	}

	@ParameterizedTest
	@CsvSource({ "10000.00, 0.14, 36", "1000.00, 0.20, 6", "99999.99, 0.16, 84", "1234.56, 0.1400, 7" })
	void elCapitalSumaElMontoYElSaldoTerminaEnCero(BigDecimal monto, BigDecimal tasa, int plazo) {
		var plan = PlanAmortizacion.generar(monto, tasa, plazo);

		assertThat(plan).hasSize(plazo);
		assertThat(plan.getLast().saldo()).isEqualByComparingTo("0");
		assertThat(plan.stream().map(PlanAmortizacion.Fila::capital).reduce(BigDecimal.ZERO, BigDecimal::add))
			.isEqualByComparingTo(monto);
	}

}
