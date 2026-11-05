package com.originacion.evaluacion;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, UUID> {

	Optional<Evaluacion> findBySolicitudId(UUID solicitudId);

}
