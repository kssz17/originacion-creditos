package com.originacion.creditos.dominio;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {

	List<EventoAuditoria> findBySolicitudIdOrderById(UUID solicitudId);

	List<EventoAuditoria> findAllByOrderByIdDesc(Pageable pagina);

	@Query("""
			select e from EventoAuditoria e
			where e.solicitudId in (select s.id from Solicitud s where s.clienteId = :clienteId)
			order by e.id desc""")
	List<EventoAuditoria> recientesDeCliente(UUID clienteId, Pageable pagina);

}
