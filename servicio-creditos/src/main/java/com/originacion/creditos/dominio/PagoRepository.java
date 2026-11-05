package com.originacion.creditos.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, UUID> {

	Optional<Pago> findBySolicitudIdAndIdempotencyKey(UUID solicitudId, String idempotencyKey);

	List<Pago> findBySolicitudIdOrderByCreadoEnDesc(UUID solicitudId);

}
