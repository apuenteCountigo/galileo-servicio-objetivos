package com.galileo.cu.objetivos.cliente;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.galileo.cu.commons.models.Balizas;
import com.galileo.cu.commons.models.Objetivos;


@FeignClient(name="servicio-apis")
public interface TraccarFeign {

	@PostMapping("/salvarObjetivoTraccar")
	public Objetivos salvar(@RequestBody Objetivos objetivos);
	
	@DeleteMapping("/eliminarObjetivoTraccar")
	String borrar(@RequestBody Objetivos objetivos);
	
	@PostMapping("/asignarBalizaObjetivoDataMiner")
	String asignarBalObj(@RequestBody Objetivos objetivos);

	@PostMapping("/estadoBalizaDataminer")
	String cambiarEstado(@RequestBody Balizas balizas);
}
