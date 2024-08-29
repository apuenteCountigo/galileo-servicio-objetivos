package com.galileo.cu.objetivos.interceptores;

import com.galileo.cu.commons.models.dto.JwtObjectMap;

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

@Component
public class ObjetivosInterceptor implements HandlerInterceptor {

	@Autowired
	private ObjectMapper objectMapper;
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException, IOException {
		System.out.println("INTERCEPTOR***" + request.getMethod() + "*******************");
		/*Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements())
			System.out.println(names.nextElement());*/

		System.out.println(request.getHeader("Authorization"));
		if (request.getMethod().equals("GET")) {
			if (!Strings.isNullOrEmpty(request.getHeader("Authorization"))) {
				String token = request.getHeader("Authorization").replace("Bearer ", "");
				System.out.println(token.toString());
				
				try {
					String[] chunks = token.split("\\.");
					Base64.Decoder decoder = Base64.getUrlDecoder();
					String header = new String(decoder.decode(chunks[0]));
					String payload = new String(decoder.decode(chunks[1]));

					System.out.println(payload.toString());

					JwtObjectMap jwtObjectMap = objectMapper.readValue(payload.toString().replace("Perfil", "perfil"),
							JwtObjectMap.class);
					System.out.println(jwtObjectMap.getId());

					System.out.println("Path:" + request.getRequestURI());
					System.out.println("Descripcion:" + jwtObjectMap.getPerfil().getDescripcion());
					if ((request.getRequestURI().equals("/listar/search/filtro")
							|| request.getRequestURI().equals("/listar/search/filtrarPorUsuario")
							|| request.getRequestURI().equals("/listar/search/dispositivo"))
							&& (jwtObjectMap.getPerfil().getDescripcion().equals("Usuario Final")
									|| jwtObjectMap.getPerfil().getDescripcion().equals("Invitado Externo"))) {
						System.out.println("-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
						System.out.println("id parametro: " + request.getParameter("idAuth"));
						if (jwtObjectMap.getId().equals(request.getParameter("idAuth"))) {
							return true;
						} else {
							System.out.println("EL USUARIO ENVIADO NO COINCIDE CON EL AUTENTICADO");
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
					System.out.println("NO HAY TOKEN");
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
				System.out.println("NO HAY TOKEN");
				response.resetBuffer();
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setHeader("Content-Type", "application/json;charset=UTF-8");
				String s="{\"errorMessage\":\"Necesita enviar un Token Válido "+request.getMethod()+" Servicio-Objetivos!\"}";
				response.getOutputStream().write(s.getBytes("UTF-8"));
				response.flushBuffer();

				return false;
			}
		}

		return true;// HandlerInterceptor.super.preHandle(request, response, handler);
	}
}
