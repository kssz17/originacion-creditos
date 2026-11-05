package com.originacion.creditos.aplicacion;

public class IdempotenciaException extends RuntimeException {

	public IdempotenciaException() {
		super("La Idempotency-Key ya se usó con otros datos");
	}

}
