package com.originacion.creditos.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Sistema francés: cuota constante, con interés decreciente sobre el saldo pendiente. */
public final class PlanAmortizacion {

	private static final BigDecimal DOCE = BigDecimal.valueOf(12);

	public record Fila(int numero, BigDecimal capital, BigDecimal interes, BigDecimal total, BigDecimal saldo) {
	}

	private PlanAmortizacion() {
	}

	public static BigDecimal cuota(BigDecimal monto, BigDecimal tasaAnual, int plazoMeses) {
		BigDecimal tasaMensual = tasaAnual.divide(DOCE, MathContext.DECIMAL64);
		BigDecimal factor = BigDecimal.ONE.add(tasaMensual).pow(plazoMeses, MathContext.DECIMAL64);
		BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(factor, MathContext.DECIMAL64));
		return monto.multiply(tasaMensual).divide(denominador, 2, RoundingMode.HALF_UP);
	}

	/** La última cuota absorbe el redondeo acumulado para que el saldo termine exactamente en cero. */
	public static List<Fila> generar(BigDecimal monto, BigDecimal tasaAnual, int plazoMeses) {
		BigDecimal tasaMensual = tasaAnual.divide(DOCE, MathContext.DECIMAL64);
		BigDecimal cuota = cuota(monto, tasaAnual, plazoMeses);
		List<Fila> filas = new ArrayList<>(plazoMeses);
		BigDecimal saldo = monto.setScale(2, RoundingMode.HALF_UP);
		for (int numero = 1; numero <= plazoMeses; numero++) {
			BigDecimal interes = saldo.multiply(tasaMensual).setScale(2, RoundingMode.HALF_UP);
			BigDecimal capital = numero == plazoMeses ? saldo : cuota.subtract(interes);
			saldo = saldo.subtract(capital);
			filas.add(new Fila(numero, capital, interes, capital.add(interes), saldo));
		}
		return filas;
	}

}
