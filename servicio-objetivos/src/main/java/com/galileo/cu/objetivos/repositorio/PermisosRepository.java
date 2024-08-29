package com.galileo.cu.objetivos.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.galileo.cu.commons.models.Permisos;

@RepositoryRestResource(exported = false)
public interface PermisosRepository extends PagingAndSortingRepository<Permisos, Long> {
	@Query("SELECT p FROM Permisos p WHERE p.tipoEntidad.id=8 AND p.idEntidad=:idEntidad")
	List<Permisos> filtar(Long idEntidad);
}
