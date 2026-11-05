package com.originacion.creditos.dominio;

import java.math.BigDecimal;

public interface SaludCartera {

	long getCreditosEnMora();

	BigDecimal getSaldoActivo();

	BigDecimal getSaldoEnMora();

}
