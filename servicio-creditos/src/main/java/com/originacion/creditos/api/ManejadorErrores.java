package com.originacion.creditos.api;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.originacion.creditos.aplicacion.IdempotenciaException;
import com.originacion.creditos.aplicacion.SolicitudNoEncontradaException;
import com.originacion.creditos.dominio.ReglaNegocioException;

@RestControllerAdvice
class ManejadorErrores {

	@ExceptionHandler({ ReglaNegocioException.class, IdempotenciaException.class })
	ProblemDetail reglaNegocio(RuntimeException error) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage());
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail edicionConcurrente() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "La solicitud cambió; vuelve a consultarla");
	}

	@ExceptionHandler(SolicitudNoEncontradaException.class)
	ProblemDetail noEncontrada() {
		return ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
	}

}
