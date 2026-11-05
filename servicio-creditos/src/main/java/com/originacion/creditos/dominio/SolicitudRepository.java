package com.originacion.creditos.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface SolicitudRepository extends JpaRepository<Solicitud, UUID> {

	Optional<Solicitud> findByClienteIdAndIdempotencyKey(UUID clienteId, String idempotencyKey);

	Page<Solicitud> findByClienteId(UUID clienteId, Pageable pagina);

	Page<Solicitud> findByEstado(EstadoSolicitud estado, Pageable pagina);

	List<Solicitud> findTop50ByEstadoAndCreadaEnBeforeOrderByCreadaEn(EstadoSolicitud estado, Instant limite);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Solicitud s where s.id = :id")
	Optional<Solicitud> bloquear(UUID id);

	@Query(nativeQuery = true, value = """
			select to_char(desembolsada_en, 'YYYY-MM') as mes, sum(monto) as monto, count(*) as creditos
			from solicitudes
			where desembolsada_en >= :desde
			group by 1
			order by 1""")
	List<DesembolsoMes> desembolsadoPorMes(LocalDate desde);

	@Query(nativeQuery = true, value = """
			with en_mora as (
				select distinct solicitud_id from cuotas where pagada_en is null and vencimiento < :hoy
			)
			select (select count(*) from en_mora) as "creditosEnMora",
				coalesce(sum(c.capital), 0) as "saldoActivo",
				coalesce(sum(c.capital) filter (where c.solicitud_id in (select solicitud_id from en_mora)), 0) as "saldoEnMora"
			from cuotas c
			join solicitudes s on s.id = c.solicitud_id
			where c.pagada_en is null and s.estado = 'DESEMBOLSADA'""")
	SaludCartera saludCartera(LocalDate hoy);

	Optional<Solicitud> findFirstByClienteIdAndDtiNotNullOrderByCreadaEnDesc(UUID clienteId);

	@Query("""
			select s.estado as estado, count(s) as total, coalesce(sum(s.monto), 0) as monto,
				coalesce(sum(s.cuotaMensual), 0) as cuotas
			from Solicitud s group by s.estado""")
	List<ResumenEstado> resumenGlobal();

	@Query("""
			select s.estado as estado, count(s) as total, coalesce(sum(s.monto), 0) as monto,
				coalesce(sum(s.cuotaMensual), 0) as cuotas
			from Solicitud s where s.clienteId = :clienteId group by s.estado""")
	List<ResumenEstado> resumenDeCliente(UUID clienteId);

}
