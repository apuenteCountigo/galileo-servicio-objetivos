package com.galileo.cu.objetivos.interceptores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.MappedInterceptor;

@Component
public class WebMvcConfig {

	@Autowired
	ObjetivosInterceptor objetivosInterceptor;

	@Bean
    public MappedInterceptor objetivosIntercept() {
        return new MappedInterceptor(new String[]{"/**"}, objetivosInterceptor);
    }
}
