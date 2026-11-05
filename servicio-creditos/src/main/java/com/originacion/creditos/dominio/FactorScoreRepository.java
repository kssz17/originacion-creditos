package com.originacion.creditos.dominio;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FactorScoreRepository extends JpaRepository<FactorScore, FactorScore.Clave> {

	List<FactorScore> findBySolicitudIdOrderByOrden(UUID solicitudId);

}
