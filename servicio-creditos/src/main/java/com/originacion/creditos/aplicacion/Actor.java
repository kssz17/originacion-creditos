package com.originacion.creditos.aplicacion;

import java.util.UUID;

/** Identidad resuelta por el gateway a partir del JWT. */
public record Actor(String usuario, String rol, UUID clienteId) {

	public boolean esAnalista() {
		return "ANALISTA".equals(rol);
	}

	public boolean esCliente() {
		return "CLIENTE".equals(rol) && clienteId != null;
	}

}
