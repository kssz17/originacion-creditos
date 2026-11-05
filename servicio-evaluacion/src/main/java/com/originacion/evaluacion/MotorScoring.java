package com.originacion.evaluacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Reglas de riesgo sin dependencias de Spring ni de la base de datos. Los umbrales son ilustrativos,
 * no una política de crédito real.
 */
public final class MotorScoring {

	private static final BigDecimal DTI_MAXIMO = new BigDecimal("0.50");

	public record Datos(BigDecimal ingresosMensuales, BigDecimal deudaMensual, int antiguedadLaboralMeses,
			BigDecimal monto, BigDecimal cuotaEstimada) {
	}

	public record Resultado(int score, BigDecimal dti, Decision decision, BigDecimal tasaAnual, String motivo,
			List<Factor> factores) {
	}

	private MotorScoring() {
	}

	public static Resultado evaluar(Datos datos) {
		BigDecimal dti = datos.deudaMensual().add(datos.cuotaEstimada())
			.divide(datos.ingresosMensuales(), 4, RoundingMode.HALF_UP);
		BigDecimal ingresosAnuales = datos.ingresosMensuales().multiply(BigDecimal.valueOf(12));
		BigDecimal montoSobreIngresos = datos.monto().divide(ingresosAnuales, 4, RoundingMode.HALF_UP);
		List<Factor> factores = List.of(new Factor("Puntuación base", 400, 400, "Parte común a todas las solicitudes"),
				new Factor("Endeudamiento", puntosPorDti(dti), 250, "%s %% de tus ingresos irá a deudas".formatted(porcentaje(dti))),
				new Factor("Antigüedad laboral", puntosPorAntiguedad(datos.antiguedadLaboralMeses()), 150,
						"%d meses en tu empleo actual".formatted(datos.antiguedadLaboralMeses())),
				new Factor("Monto solicitado", puntosPorMonto(montoSobreIngresos), 100,
						"%s %% de tus ingresos anuales".formatted(porcentaje(montoSobreIngresos))));
		int score = factores.stream().mapToInt(Factor::puntos).sum();

		if (dti.compareTo(DTI_MAXIMO) > 0) {
			return new Resultado(score, dti, Decision.RECHAZADA, null, "Endeudamiento superior al 50% de los ingresos",
					factores);
		}
		if (score >= 700) {
			return new Resultado(score, dti, Decision.APROBADA, tasaPara(score), "Perfil dentro de la política automática",
					factores);
		}
		if (score >= 550) {
			return new Resultado(score, dti, Decision.REVISION_MANUAL, tasaPara(score), "Requiere revisión de un analista",
					factores);
		}
		return new Resultado(score, dti, Decision.RECHAZADA, null, "Score inferior al mínimo de 550", factores);
	}

	private static String porcentaje(BigDecimal proporcion) {
		return proporcion.movePointRight(2).setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
	}

	private static int puntosPorDti(BigDecimal dti) {
		if (dti.compareTo(new BigDecimal("0.20")) <= 0) {
			return 250;
		}
		if (dti.compareTo(new BigDecimal("0.35")) <= 0) {
			return 150;
		}
		return dti.compareTo(DTI_MAXIMO) <= 0 ? 50 : 0;
	}

	private static int puntosPorAntiguedad(int meses) {
		if (meses >= 36) {
			return 150;
		}
		return meses >= 12 ? 80 : 0;
	}

	private static int puntosPorMonto(BigDecimal montoSobreIngresos) {
		if (montoSobreIngresos.compareTo(new BigDecimal("0.50")) <= 0) {
			return 100;
		}
		return montoSobreIngresos.compareTo(BigDecimal.ONE) <= 0 ? 50 : 0;
	}

	private static BigDecimal tasaPara(int score) {
		if (score >= 800) {
			return new BigDecimal("0.1400");
		}
		return score >= 700 ? new BigDecimal("0.1600") : new BigDecimal("0.2000");
	}

}
