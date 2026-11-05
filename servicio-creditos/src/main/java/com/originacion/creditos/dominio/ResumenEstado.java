package com.originacion.creditos.dominio;

import java.math.BigDecimal;

public interface ResumenEstado {

	EstadoSolicitud getEstado();

	long getTotal();

	BigDecimal getMonto();

	BigDecimal getCuotas();

}
