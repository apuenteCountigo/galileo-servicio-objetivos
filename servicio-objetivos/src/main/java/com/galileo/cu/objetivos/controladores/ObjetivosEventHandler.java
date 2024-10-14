package com.galileo.cu.objetivos.controladores;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galileo.cu.commons.models.AccionEntidad;
import com.galileo.cu.commons.models.Balizas;
import com.galileo.cu.commons.models.HistoricoObjetivosBalizas;
import com.galileo.cu.commons.models.Obj;
import com.galileo.cu.commons.models.Objetivos;
import com.galileo.cu.commons.models.Operaciones;
import com.galileo.cu.commons.models.Permisos;
import com.galileo.cu.commons.models.TipoEntidad;
import com.galileo.cu.commons.models.Trazas;
import com.galileo.cu.commons.models.Usuarios;
import com.galileo.cu.objetivos.cliente.TraccarFeign;
import com.galileo.cu.objetivos.repositorio.BalizasRepository;
import com.galileo.cu.objetivos.repositorio.EstadosRepository;
import com.galileo.cu.objetivos.repositorio.HistoricoObjetivosBalizasRepository;
import com.galileo.cu.objetivos.repositorio.ObjetivosRepository;
import com.galileo.cu.objetivos.repositorio.OperacionesRepository;
import com.galileo.cu.objetivos.repositorio.PermisosRepository;
import com.galileo.cu.objetivos.repositorio.TrazasRepository;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RepositoryEventHandler(Obj.class)
public class ObjetivosEventHandler {
	@Autowired
	private TraccarFeign traccar;

	@Autowired
	private BalizasRepository balizasRepository;

	@Autowired
	private EstadosRepository estadosrepo;

	@Autowired
	private ObjetivosRepository objetivosrepo;

	@Autowired
	private OperacionesRepository opRepo;

	@Autowired
	private HistoricoObjetivosBalizasRepository histObjBalRepo;

	@Autowired
	private PermisosRepository permisosRepo;

	@Autowired
	private ObjectMapper objectMapper;

	private HttpServletRequest req;

	long idBalizaTmp = 0;

	@Autowired
	EntityManager entMg;

	@Autowired
	TrazasRepository trazasRepo;

	public ObjetivosEventHandler(HttpServletRequest request) {
		this.req = request;
	}

	@HandleBeforeCreate
	public void handleObjetivosCreate(Objetivos obj) {
		this.req.setAttribute("handleBD", false);
		this.req.setAttribute("handleBeforeCreate", false);
		this.req.setAttribute("handleAfterCreate", false);
		this.req.setAttribute("isTraccarInserted", false);
		this.req.setAttribute("objetivo", obj);

		try {
			log.info("SERVICIO OBJETIVOS EJECUTANDO HandleBeforeCreate");
			if (obj.getBalizas() != null) {
				Balizas be = balizasRepository.findById(obj.getBalizas().getId()).get();
				log.info("Validando Baliza Operativa: " + be.toString());
				String est = be.getEstados().getDescripcion();
				if (est.equals("Operativa") || est.equals("En Instalación")) {
					log.error("Fallo La Baliza está en Uso");
					throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "01", null);
				}
			}
		} catch (Exception e) {
			log.error("Fallo handleObjetivosCreate " + e.getMessage());
			if (e.getMessage().equals("500 INTERNAL_SERVER_ERROR \"01\"")) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La Baliza está en Uso",
						null);
			} else {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Fallo Validando la Baliza Asignada al Objetivo", null);
			}
		}

		try {
			Objetivos objetivoUpdate = traccar.salvar(obj);
			obj.setTraccarID(objetivoUpdate.getTraccarID());
			this.req.setAttribute("isTraccarInserted", true);
		} catch (Exception e) {
			log.error("Fallo al Insertar Objetivo en el Traccar " + e.getMessage());
			throw new RuntimeException("Fallo al Insertar Objetivo en el Traccar ");
		}

		this.req.setAttribute("handleBeforeCreate", true);
		if (!Strings.isNullOrEmpty(obj.getDescripcion()) && obj.getDescripcion().equals("BDFail"))
			throw new RuntimeException("Fallo insertando objetivo en la bd.");
	}

	@HandleAfterCreate
	public void handleObjetivosAfterCreate(Objetivos obj) {
		this.req.setAttribute("handleBD", true);
		/* Validando Autorización */
		// ValidateAuthorization val = new ValidateAuthorization();
		// try {
		// System.out.println("REQUEST HandleAfterCreate: " + req.getMethod());
		// val.setObjectMapper(objectMapper);
		// val.setReq(req);
		// if (!val.Validate()) {
		// throw new RuntimeException("Fallo de Autorización");
		// }
		// } catch (Exception e) {
		// System.out.println("Fallo Despues de Crear Objetivo Validando Autorización: "
		// + e.getMessage());
		// throw new RuntimeException("Fallo Despues de Crear Objetivo Validando
		// Autorización: ");
		// }

		// Cambio de Estado de Baliza Asignada
		try {
			if (obj != null && obj.getBalizas() != null) {
				Balizas b = balizasRepository.findById(obj.getBalizas().getId()).get();
				b.setEstados(estadosrepo.findByDescripcion("En Instalación"));
				b.setFechaAsignaOp(LocalDateTime.now());
				Operaciones op = opRepo.findById(obj.getOperaciones().getId()).get();
				log.info("Baliza " + b.getClave() + " con Operación " + op.getDescripcion());
				b.setOperacion(op.getDescripcion());
				log.info("Baliza " + b.getClave() + " con Objetivo " + obj.getDescripcion());
				b.setObjetivo(obj.getDescripcion());
				balizasRepository.save(b);

				// ActualizarTrazaObjetivo(val, (int) b.getId(), 3, 3, "Fue Asignada la Baliza:
				// " + b.getClave(),
				// "Fallo Insertando la Asignación de Baliza al Objetivo: " +
				// obj.getDescripcion()
				// + " en la Trazabilidad");
			}
		} catch (Exception e) {
			log.error("Fallo Actualizando Estado de Baliza al Crear Objetivo " + e.getMessage());
			throw new RuntimeException("Fallo Actualizando Estado de Baliza al Crear Objetivo ");
		}

		try {
			log.info("antes de crear HistoricoObjetivosBalizas");
			if (obj.getBalizas() != null) {
				HistoricoObjetivosBalizas historico = new HistoricoObjetivosBalizas();
				// Optional<Balizas> b=balizasRepository.findById(obj.getBalizas().getId());
				Balizas b = new Balizas();
				b.setId(obj.getBalizas().getId());
				historico.setBaliza(b);
				Objetivos oj = new Objetivos();
				oj.setId(obj.getId());
				historico.setObjetivo(oj);
				AccionEntidad act = new AccionEntidad();
				act.setId(4);
				historico.setAccion(act);
				histObjBalRepo.save(historico);
			}
		} catch (Exception e) {
			log.error("Fallo al insertar en HistoricoObjetivosBalizas " + e.getMessage());
			throw new RuntimeException("Fallo al insertar en HistoricoObjetivosBalizas");
		}

		// ActualizarTrazaObjetivo(val, obj.getId().intValue(), 8, 1, "Fue Creado el
		// Objetivo: " + obj.getDescripcion(),
		// "Fallo Insertando la Creación del Objetivo: " + obj.getDescripcion() + " en
		// la Trazabilidad");
		this.req.setAttribute("handleAfterCreate", true);
	}

	@HandleBeforeSave
	public void handleObjetivosSave(Objetivos obj) {
		/* Validando Autorización */
		// ValidateAuthorization val = new ValidateAuthorization();
		// try {
		// System.out.println("REQUEST HandleBeforeSave: " + req.getMethod());
		// val.setObjectMapper(objectMapper);
		// val.setReq(req);
		// if (!val.Validate()) {
		// throw new RuntimeException("Fallo de Autorización");
		// }
		// } catch (Exception e) {
		// System.out.println("Fallo Antes de Actualizar Objetivo Validando
		// Autorización: " + e.getMessage());
		// throw new RuntimeException("Fallo Antes de Actualizar Objetivo Validando
		// Autorización: ");
		// }

		entMg.detach(obj);

		try {
			long traccarID = objetivosrepo.findById(obj.getId()).get().getTraccarID();
			obj.setTraccarID(traccarID);
		} catch (Exception e) {
			String err = "Fallo, obteniendo id de traccar";
			log.error("{}: {}", err, e.getMessage());
			if (e.getMessage().contains(".getTraccarID()\" is null")) {
				err = "Fallo, el objetivo no tiene id de traccar, consulte a un administrador para solucionarlo";
			}
			throw new RuntimeException(err);
		}

		try {
			if (obj.getBalizas() != null) {
				Balizas be = balizasRepository.findById(obj.getBalizas().getId()).get();
				System.out.println("Validando Baliza Operativa: " + be.toString());
				if (be != null) {
					String est = be.getEstados().getDescripcion();
					if ((est.equals("Operativa") || est.equals("En Instalación"))
							&& (objetivosrepo.findById(obj.getId()).get().getBalizas() == null
									|| objetivosrepo.findById(obj.getId()).get().getBalizas().getId() != be.getId())) {
						System.out.println("Fallo La Baliza está en Uso");
						throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "01", null);
					}
				}
			}
		} catch (Exception e) {
			if (e.getMessage().equals("500 INTERNAL_SERVER_ERROR \"01\"")) {
				System.out.println("La Baliza está en Uso");
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La Baliza está en Uso",
						e);
			} else {
				System.out.println("Fallo Validando Baliza " + e.getMessage());
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fallo Validando Baliza", e);
			}
		}

		if (obj != null && obj.getBalizas() != null) {
			Balizas b = balizasRepository.findById(obj.getBalizas().getId()).get();
			Balizas bTmp = b;
			long bEstado = b.getEstados().getId();
			b.setEstados(estadosrepo.findByDescripcion("En Instalación"));
			b.setFechaAsignaOp(LocalDateTime.now());
			Operaciones op = opRepo.findById(obj.getOperaciones().getId()).get();
			System.out.println("Baliza " + b.getClave() + " con Operación " + op.getDescripcion());
			b.setOperacion(op.getDescripcion());
			System.out.println("Baliza " + b.getClave() + " con Objetivo " + obj.getDescripcion());
			b.setObjetivo(obj.getDescripcion());
			try {
				balizasRepository.save(b);
			} catch (Exception e) {
				System.out.println("Fallo Cambiando el Estado de la Baliza: " + b.getClave()
						+ " en Base de Datos: " + e.getMessage());
				throw new RuntimeException(
						"Fallo Cambiando el Estado de la Baliza: " + b.getClave() + " en Base de Datos ");
			}
			try {
				traccar.cambiarEstado(b);
			} catch (Exception e) {
				balizasRepository.save(bTmp);
				System.out.println("Fallo Cambiando el Estado de la Baliza: " + b.getClave() + " en Dataminer: "
						+ e.getMessage());
				throw new RuntimeException(
						"Fallo Cambiando el Estado de la Baliza: " + b.getClave() + " en Dataminer: ");
			}
		}

		System.out.println("Antes de Actualizar HistoricoObjetivosBalizas");
		Objetivos objTmp = objetivosrepo.findById(obj.getId()).get();

		if (objTmp != null && objTmp.getBalizas() != null && obj != null
				&& (obj.getBalizas() == null || objTmp.getBalizas() != obj.getBalizas())) {
			System.out.println(objTmp.getBalizas().toString());
			idBalizaTmp = objTmp.getBalizas().getId();

			Balizas bOld = balizasRepository.findById(objTmp.getBalizas().getId()).get();
			Balizas bOldTmp = bOld;
			// Desasignando Baliza
			bOld.setEstados(estadosrepo.findByDescripcion("Disponible en Unidad"));
			bOld.setOperacion(null);
			bOld.setObjetivo(null);

			try {
				balizasRepository.save(bOld);
			} catch (Exception e) {
				System.out.println("Fallo Cambiando el Estado de la Baliza: " + bOld.getClave()
						+ " en Base de Datos: " + e.getMessage());
				throw new RuntimeException(
						"Fallo Cambiando el Estado de la Baliza: " + bOld.getClave() + " en Base de Datos ");
			}

			try {
				traccar.cambiarEstado(bOld);
			} catch (Exception e) {
				balizasRepository.save(bOldTmp);
				System.out.println("Fallo Cambiando el Estado de la Baliza: " + bOld.getClave()
						+ " en Dataminer: " + e.getMessage());
				throw new RuntimeException(
						"Fallo Cambiando el Estado de la Baliza: " + bOld.getClave() + " en Dataminer ");
			}
		} else
			idBalizaTmp = 0;
	}

	@HandleAfterSave
	public void HandleObjetivosAfterSave(Objetivos obj) {
		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			System.out.println("REQUEST HandleAfterSave: " + req.getMethod());
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				System.out.println("Fallo de Autorización Actualizar de Salvar Objetivo");
				throw new RuntimeException("Fallo de Autorización Despues de Actualizar Objetivo");
			}
		} catch (Exception e) {
			System.out.println("Fallo Despues de Actualizar Objetivo Validando Autorización: " + e.getMessage());
			throw new RuntimeException("Fallo Despues de Actualizar Objetivo Validando Autorización: ");
		}

		if (idBalizaTmp > 0) {
			Balizas bOld = new Balizas();
			bOld = balizasRepository.findById(idBalizaTmp).get();

			HistoricoObjetivosBalizas historico = new HistoricoObjetivosBalizas();
			historico.setBaliza(bOld);
			historico.setObjetivo(obj);
			AccionEntidad act = new AccionEntidad();
			act.setId(5);
			historico.setAccion(act);
			histObjBalRepo.save(historico);

			ActualizarTrazaObjetivo(val, (int) bOld.getId(), 3, 5,
					"Fue Desasignada la Baliza: " + bOld.getClave() + " del Objetivo " + obj.getDescripcion(),
					"Fallo Insertando la Desasignación de Baliza al Objetivo: " + obj.getDescripcion()
							+ " en la Trazabilidad");
		}

		try {
			if (obj != null && obj.getBalizas() != null) {
				Balizas b = balizasRepository.findById(obj.getBalizas().getId()).get();

				HistoricoObjetivosBalizas historico = new HistoricoObjetivosBalizas();
				historico.setBaliza(obj.getBalizas());
				historico.setObjetivo(obj);
				AccionEntidad act = new AccionEntidad();
				act.setId(4);
				historico.setAccion(act);
				histObjBalRepo.save(historico);

				ActualizarTrazaObjetivo(val, (int) b.getId(), 3, 4,
						"Fue Asignada la Baliza: " + b.getClave() + " Al Objetivo " + obj.getDescripcion(),
						"Fallo Insertando la Asignación de Baliza al Objetivo: " + obj.getDescripcion()
								+ " en la Trazabilidad");
			}
			ActualizarTrazaObjetivo(val, obj.getId().intValue(), 8, 1,
					"Fue Actualizado el Objetivo: " + obj.getDescripcion(),
					"Fallo Insertando la Creación del Objetivo: " + obj.getDescripcion() + " en la Trazabilidad");
		} catch (Exception e) {
			System.out.println("Fallo Actualizando Estado de Baliza al Actualizar Objetivo " + e.getMessage());
			throw new RuntimeException("Fallo Actualizando Estado de Baliza al Actualizar Objetivo ");
		}
	}

	@HandleBeforeDelete
	public void handleObjetivosDelete(Objetivos objetivo) {
		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			System.out.println("REQUEST HandleBeforeDelete: " + req.getMethod());
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				throw new RuntimeException("Fallo de Autorización");
			}
		} catch (Exception e) {
			System.out.println("Fallo Antes de Eliminar Objetivo Validando Autorización: " + e.getMessage());
			throw new RuntimeException("Fallo Antes de Eliminar Objetivo Validando Autorización: ");
		}

		try {
			long traccarID = objetivosrepo.findById(objetivo.getId()).get().getTraccarID();
			objetivo.setTraccarID(traccarID);
		} catch (Exception e) {
			String err = "Fallo, obteniendo id de traccar";
			log.error("{}: {}", err, e.getMessage());
			if (e.getMessage().contains(".getTraccarID()\" is null")) {
				objetivo.setTraccarID((long) 0);
			}
			throw new RuntimeException(err);
		}

		try {
			traccar.borrar(objetivo);
		} catch (Exception e) {
			System.out.println("Fallo Eliminando Objetivo en las APIS");
			System.out.println(e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fallo Eliminando Objetivo en las APIS",
					e);
		}
	}

	@HandleAfterDelete
	public void handleObjetivosAfterDelete(Objetivos obj) throws IOException {
		System.out.println("entro despues delete objetivo");
		/*
		 * OriginCascading originCascading = new OriginCascading();
		 * System.out.println("Objetivos @HandleBeforeDelete");
		 * String s = IOUtils.toString(req.getReader());
		 * if (s != null && s != "") {
		 * System.out.println("HandleObjetivosgetReader");
		 * System.out.println(s);
		 * originCascading = objectMapper.readValue(s, OriginCascading.class);
		 * System.out.println("OriginCascading=" + originCascading.origin);
		 * }
		 */

		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			System.out.println("REQUEST HandleAfterDelete: " + req.getMethod());
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				throw new RuntimeException("Fallo de Autorización");
			}
		} catch (Exception e) {
			System.out.println("Fallo Después de Eliminar Objetivo Validando Autorización: " + e.getMessage());
			throw new RuntimeException("Fallo Después de Eliminar Objetivo Validando Autorización: ");
		}

		List<HistoricoObjetivosBalizas> historicos = histObjBalRepo.listaHistObjBalizas(obj.getId());
		histObjBalRepo.deleteAll(historicos);

		List<Permisos> permisos = permisosRepo.filtar(obj.getId());
		permisosRepo.deleteAll(permisos);

		Balizas baliza = obj.getBalizas();
		if (baliza != null) {
			baliza.setEstados(estadosrepo.findByDescripcion("Disponible en Unidad"));
			Operaciones op = opRepo.findById(obj.getOperaciones().getId()).get();
			baliza.setOperacion(null);
			baliza.setObjetivo(null);
			System.out.println("Balizas: " + baliza.toString());
			try {
				traccar.cambiarEstado(baliza);
			} catch (Exception e) {
				System.out.println("Fallo Cambiando el Estado de la Baliza: " + baliza.getClave() + " en Dataminer: "
						+ e.getMessage());
				throw new RuntimeException(
						"Fallo Cambiando el Estado de la Baliza: " + baliza.getClave() + " en Dataminer: ");
			}
			balizasRepository.save(baliza);

			ActualizarTrazaEstadoBaliza(val, baliza, 3, 3,
					"Fue Actualizado al Estado: " + baliza.getEstados().getDescripcion()
							+ " de la Baliza: " + baliza.getClave(),
					"Fallo al Insertar la Actualización de la Baliza " + baliza.getClave() + " en la Trazabilidad");
		}

		ActualizarTrazaObjetivo(val, obj.getId().intValue(), 8, 2, "Fue Eliminado el Objetivo: " + obj.getDescripcion(),
				"Fallo Insertando la Eliminación del Objetivo: " + obj.getDescripcion() + " en la Trazabilidad");
	}

	private void ActualizarTrazaEstadoBaliza(ValidateAuthorization val, Balizas baliza, int idEntidad, int idAccion,
			String trazaDescripcion, String errorMessage) {
		try {
			System.out.println("Actualizar el Estado de la Baliza en la Trazabilidad AfterDelete");
			Trazas traza = new Trazas();
			AccionEntidad accion = new AccionEntidad();
			Usuarios usuario = new Usuarios();
			TipoEntidad entidad = new TipoEntidad();

			entidad.setId(idEntidad);
			accion.setId(idAccion);
			usuario.setId(Long.parseLong(val.getJwtObjectMap().getId()));

			traza.setAccionEntidad(accion);
			traza.setTipoEntidad(entidad);
			traza.setUsuario(usuario);
			traza.setIdEntidad((int) baliza.getId());
			traza.setDescripcion(trazaDescripcion);
			trazasRepo.save(traza);

		} catch (Exception e) {
			System.out.println(errorMessage);
			System.out.println(e.getMessage());
			throw new RuntimeException(errorMessage);
		}
	}

	private void ActualizarTrazaObjetivo(ValidateAuthorization val, int idEntidad, int idTipoEntidad,
			int idAccion, String trazaDescripcion, String errorMessage) {
		try {
			System.out.println("Eliminar el Objetivo en la Trazabilidad AfterDelete");
			Trazas traza = new Trazas();
			AccionEntidad accion = new AccionEntidad();
			Usuarios usuario = new Usuarios();
			TipoEntidad entidad = new TipoEntidad();

			entidad.setId(idTipoEntidad);
			accion.setId(idAccion);
			usuario.setId(Long.parseLong(val.getJwtObjectMap().getId()));

			traza.setAccionEntidad(accion);
			traza.setTipoEntidad(entidad);
			traza.setUsuario(usuario);
			traza.setIdEntidad(idEntidad);
			traza.setDescripcion(trazaDescripcion);
			trazasRepo.save(traza);
		} catch (Exception e) {
			System.out.println(errorMessage);// "Fallo al Insertar la Eliminación del Objetivo en la Trazabilidad"
			System.out.println(e.getMessage());
			throw new RuntimeException(errorMessage);
		}
	}
}
