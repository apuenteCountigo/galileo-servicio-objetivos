package com.galileo.cu.objetivos.interceptores;

import com.galileo.cu.commons.models.Objetivos;
import com.galileo.cu.commons.models.dto.JwtObjectMap;
import com.galileo.cu.objetivos.cliente.TraccarFeign;

import java.io.IOException;

import java.util.Base64;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ObjetivosInterceptor implements HandlerInterceptor {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TraccarFeign traccar;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException, IOException {
		/*
		 * Enumeration<String> names = request.getHeaderNames();
		 * while (names.hasMoreElements())
		 * System.out.println(names.nextElement());
		 */

		if (request.getMethod().equals("GET")) {
			if (!Strings.isNullOrEmpty(request.getHeader("Authorization"))) {
				String token = request.getHeader("Authorization").replace("Bearer ", "");

				try {
					String[] chunks = token.split("\\.");
					Base64.Decoder decoder = Base64.getUrlDecoder();
					String header = new String(decoder.decode(chunks[0]));
					String payload = new String(decoder.decode(chunks[1]));

					JwtObjectMap jwtObjectMap = objectMapper.readValue(payload.toString().replace("Perfil", "perfil"),
							JwtObjectMap.class);

					if ((request.getRequestURI().equals("/listar/search/filtro")
							|| request.getRequestURI().equals("/listar/search/filtrarPorUsuario")
							|| request.getRequestURI().equals("/listar/search/dispositivo"))
							&& (jwtObjectMap.getPerfil().getDescripcion().equals("Usuario Final")
									|| jwtObjectMap.getPerfil().getDescripcion().equals("Invitado Externo"))) {
						if (jwtObjectMap.getId().equals(request.getParameter("idAuth"))) {
							return true;
						} else {
							log.error("EL USUARIO ENVIADO NO COINCIDE CON EL AUTENTICADO");
							response.resetBuffer();
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setHeader("Content-Type", "application/json;charset=UTF-8");
							response.getOutputStream()
									.write("{\"errorMessage\":\"EL USUARIO ENVIADO NO COINCIDE CON EL AUTENTICADO!\"}"
											.getBytes("UTF-8"));
							response.flushBuffer();

							return false;
						}
					}
				} catch (Exception e) {
					log.error("NO HAY TOKEN");
					response.resetBuffer();
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.setHeader("Content-Type", "application/json;charset=UTF-8");
					String s = "{\"errorMessage\":\"ERROR en Interceptor de Seguriad Servicio-Objetivos\",\"errorOficial\":\""
							+ e.getMessage() + "\"}";
					response.getOutputStream().write(s.getBytes("UTF-8"));
					response.flushBuffer();
					return false;
				}

			} else {
				log.error("NO HAY TOKEN");
				response.resetBuffer();
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setHeader("Content-Type", "application/json;charset=UTF-8");
				String s = "{\"errorMessage\":\"Necesita enviar un Token Válido " + request.getMethod()
						+ " Servicio-Objetivos!\"}";
				response.getOutputStream().write(s.getBytes("UTF-8"));
				response.flushBuffer();

				return false;
			}
		}

		return true;// HandlerInterceptor.super.preHandle(request, response, handler);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		Objetivos objs = (Objetivos) request.getAttribute("objetivo");
		boolean handleBeforeCreate = request.getAttribute("handleBeforeCreate") != null
				? (boolean) request.getAttribute("handleBeforeCreate")
				: false;
		boolean handleAfterCreate = request.getAttribute("handleAfterCreate") != null
				? (boolean) request.getAttribute("handleAfterCreate")
				: false;
		boolean handleBD = request.getAttribute("handleBD") != null ? (boolean) request.getAttribute("handleBD")
				: false;
		boolean isTraccarInserted = request.getAttribute("isTraccarInserted") != null
				? (boolean) request.getAttribute("isTraccarInserted")
				: false;

		// if (isTraccarInserted && !handleBD) {
		// try {
		// log.info("Iniciando el Rollback por fallo al insertar objetivo en la bd.");
		// traccar.borrar(objs);
		// log.info("Finalizado el Rollback por fallo al insertar objetivo en la bd.");
		// } catch (Exception e) {
		// String err = "Fallo eliminando grupo en traccar, ejecutando el rollback por
		// fallo al insertar el objetivo en la bd.";
		// log.error("{} : {}", err, e.getMessage());
		// }
		// }
	}
}
