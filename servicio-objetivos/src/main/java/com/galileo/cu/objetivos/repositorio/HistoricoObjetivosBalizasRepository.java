package com.galileo.cu.objetivos.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.galileo.cu.commons.models.HistoricoObjetivosBalizas;

@RepositoryRestResource(exported = false)
public interface HistoricoObjetivosBalizasRepository extends CrudRepository<HistoricoObjetivosBalizas, Long> {
	@Query("SELECT h FROM HistoricoObjetivosBalizas h WHERE h.objetivo.Id=:idobjetivo")
	public List<HistoricoObjetivosBalizas> listaHistObjBalizas(long idobjetivo);
}
