package com.galileo.cu.objetivos.repositorio;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.galileo.cu.commons.models.Trazas;

@RepositoryRestResource(exported = false)
public interface TrazasRepository extends PagingAndSortingRepository<Trazas, Long> {
    
}
