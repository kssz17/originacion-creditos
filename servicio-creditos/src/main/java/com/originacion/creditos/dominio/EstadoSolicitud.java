package com.originacion.creditos.dominio;

public enum EstadoSolicitud {

	RECIBIDA, EN_REVISION, APROBADA, RECHAZADA, DESEMBOLSADA, LIQUIDADA;

	public boolean puedePasarA(EstadoSolicitud destino) {
		return switch (this) {
			case RECIBIDA -> destino == EN_REVISION || destino == APROBADA || destino == RECHAZADA;
			case EN_REVISION -> destino == APROBADA || destino == RECHAZADA;
			case APROBADA -> destino == DESEMBOLSADA;
			case DESEMBOLSADA -> destino == LIQUIDADA;
			case RECHAZADA, LIQUIDADA -> false;
		};
	}

}
