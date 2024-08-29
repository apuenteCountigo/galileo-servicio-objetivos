package com.galileo.cu.objetivos.repositorio;



import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.galileo.cu.commons.models.Objetivos;


@CrossOrigin(origins = "*")
@RepositoryRestResource(collectionResourceRel = "objetivos", path = "listar")
public interface ObjetivosRepository extends PagingAndSortingRepository<Objetivos, Long> {
	@Query("SELECT o FROM Objetivos o " 
			+ "WHERE (:descripcion='' or o.descripcion like %:descripcion%) "
			+ "AND (:idoperacion=0 or o.operaciones.id=:idoperacion) "
			+ " AND ( "
			+ "			(:idAuth IN (SELECT up FROM Usuarios up WHERE up.perfil.id=1)) "
			+ "	OR ( "
			+ "		(:idAuth IN (SELECT up FROM Usuarios up WHERE up.perfil.id=2)) "
			+ "		AND (o.operaciones.unidades.id=(SELECT unidad.id FROM UnidadesUsuarios WHERE usuario.id=:idAuth AND estado.id=6)) "
			+ "		) "
			+ " OR ("
			+ "		(:idAuth IN (SELECT id FROM Usuarios WHERE perfil.id>2)) "
			+ "		AND ("
			+ "			o.id IN (SELECT idEntidad FROM Permisos WHERE tipoEntidad.id=8 AND usuarios.id=:idAuth) "
			+ "			OR "
			+ "			o.operaciones.id IN (SELECT idEntidad FROM Permisos where tipoEntidad.id=6 AND usuarios.id=:idAuth)"
			+ "			) "
			+ " 	) "
			+ ") "
			+ "AND ((:fechaFin!=null and :fechaInicio!=null and o.fechaCreacion between :fechaInicio and :fechaFin) "
			+ "OR (:fechaFin=null and :fechaInicio!=null and o.fechaCreacion >=:fechaInicio) "
			+ "OR (:fechaFin=null and :fechaInicio=null)) "
	        + "AND (:id=0 or o.Id=:id) ")
	public Page<Objetivos> filtrar(long idAuth, String descripcion, int idoperacion, int id, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin, Pageable p);
	
	@Query("SELECT o FROM Objetivos o WHERE "
				+" o.Id IN( Select idEntidad from Permisos where tipoEntidad=8 and (:idUsuario=0 or usuarios.id=:idUsuario)) "
				+ "and (:idObjetivo =0 or o.Id=:idObjetivo)")
				Page<Objetivos> filtrarPorUsuario(int idUsuario,int idObjetivo, Pageable p);
	 
	 @Query("SELECT o FROM Objetivos o JOIN o.operaciones op WHERE "
			 +"(:perfil in (3,4,0) "
				+"and  o.Id IN( Select idEntidad from Permisos where tipoEntidad=8 and (:idUsuario=0 or usuarios.id=:idUsuario)) "
				+"and :idUsuario IN( Select usu from UnidadesUsuarios uu LEFT JOIN uu.usuario usu WHERE uu.unidad.Id=:unidad)) "
				+ "or (:perfil=2 "
				+"and :idUsuario IN( Select usu from UnidadesUsuarios uu LEFT JOIN uu.usuario usu WHERE uu.unidad.Id=:unidad) "
				+ ") or (:perfil=1 "							
				+ " ) "
				+ "and op.unidades.Id=:unidad "	
				+ "and (:descripcion='' or o.descripcion like %:descripcion%) "
				+ "AND (:idoperacion=0 or o.operaciones.Id=:idoperacion) "
				+ "AND (:id=0 or o.Id=:id) "
				+ "and ((:fechaFin!=null and :fechaInicio!=null and o.fechaCreacion between :fechaInicio and :fechaFin) "
				+ "or (:fechaFin=null and :fechaInicio!=null and o.fechaCreacion >=:fechaInicio) "
				+ "or (:fechaFin=null and :fechaInicio=null)) "
				)	 
			Page<Objetivos> filtro(int perfil,long unidad,int idUsuario,String descripcion, int idoperacion, int id,@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin, Pageable p);
	 
	 	@Query(" select o from Objetivos o  where "
	 			+ "("
	 			+ "	(:idAuth IN (SELECT id FROM Usuarios WHERE perfil.id=1)) "
	 			+ "	OR ( "
				+ "		(:idAuth IN (SELECT id FROM Usuarios WHERE perfil.id=2)) "
				+ "		AND ( "
				+ "				(o.operaciones.unidades.id=(SELECT unidad.id FROM UnidadesUsuarios WHERE usuario.id=:idAuth AND estado.id=6)) "
				+ "			OR "
				+ "				(o.id IN (SELECT idEntidad FROM Permisos WHERE tipoEntidad.id=8 AND usuarios.id=:idAuth) ) "
				+ "			OR "
				+ "				(o.operaciones.id IN (SELECT idEntidad FROM Permisos WHERE tipoEntidad.id=6 AND usuarios.id=:idAuth) ) "
				+ "			)"
				+ "		) "
				+ "	OR ( "
				+ "		(:idAuth IN (SELECT id FROM Usuarios WHERE perfil.id>2)) "
				+ "		AND ( "
				+ "				(o.id IN (SELECT idEntidad FROM Permisos WHERE tipoEntidad.id=8 AND usuarios.id=:idAuth) ) "
				+ "			OR "
				+ "				(o.operaciones.id IN (SELECT idEntidad FROM Permisos WHERE tipoEntidad.id=6 AND usuarios.id=:idAuth) ) "
				+ "			)"
				+ "		) "
				+ ")"
	 			+ "AND (:nombreObjetivo='' or o.descripcion like %:nombreObjetivo%) "
	 			+ "AND (:nombreOperacion='' or o.operaciones.descripcion like %:nombreOperacion%) "
	 			+ "AND (:nombreUnidad='' or o.operaciones.unidades.denominacion like %:nombreUnidad%) "
	 			+ "AND (:clave='' or o.balizas.clave like %:clave%) ")
		public Page<Objetivos> dispositivo(long idAuth,String clave,String nombreUnidad,String nombreOperacion,String nombreObjetivo,Pageable p);
}