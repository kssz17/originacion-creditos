package com.originacion.creditos.dominio;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CuotaRepository extends JpaRepository<Cuota, Cuota.Clave> {

	List<Cuota> findBySolicitudIdOrderByNumero(UUID solicitudId);

	List<Cuota> findBySolicitudIdAndPagadaEnIsNullOrderByNumero(UUID solicitudId);

	@Query("""
			select c from Cuota c
			where c.pagadaEn is null
				and c.solicitudId in (select s.id from Solicitud s where s.clienteId = :clienteId and s.estado = :estado)
			order by c.vencimiento""")
	List<Cuota> pendientesDeCliente(UUID clienteId, EstadoSolicitud estado);

}
