package com.originacion.evaluacion;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Factor(@Column(nullable = false) String nombre, @Column(nullable = false) int puntos,
		@Column(nullable = false) int maximo, @Column(nullable = false) String detalle) {
}
