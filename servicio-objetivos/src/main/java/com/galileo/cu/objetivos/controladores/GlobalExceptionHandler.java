package com.galileo.cu.objetivos.controladores;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        if (ex.getMessage().contains("Fallo") || ex.getMessage().contains(" es nulo")) {
            return new ResponseEntity<>("{\"message\":\"" + ex.getMessage() + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (ex.getMessage().contains("could not execute statement;")) {
            if (ex.getMessage().contains("constraint [uk_descripcion_idOperacion];")) {
                String err = "{\"message\":\"Fallo, ya existe un objetivo con este nombre, en esta unidad.\"}";
                log.error(err, ex);
                return new ResponseEntity<>(err, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        // Personaliza el mensaje de error
        log.error(ex.getMessage());
        return new ResponseEntity<>("{\"message\":\"Ocurrió un error inesperado\"}",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
