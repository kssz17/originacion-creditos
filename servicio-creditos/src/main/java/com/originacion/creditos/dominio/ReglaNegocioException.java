package com.originacion.creditos.dominio;

/** Operación válida en forma pero que el estado actual del negocio no permite. */
public class ReglaNegocioException extends RuntimeException {

	public ReglaNegocioException(String mensaje) {
		super(mensaje);
	}

}
