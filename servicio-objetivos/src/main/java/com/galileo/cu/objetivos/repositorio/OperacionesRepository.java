package com.galileo.cu.objetivos.repositorio;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.galileo.cu.commons.models.Operaciones;

@RepositoryRestResource(exported = false)
public interface OperacionesRepository extends CrudRepository<Operaciones, Long>{
    
}
