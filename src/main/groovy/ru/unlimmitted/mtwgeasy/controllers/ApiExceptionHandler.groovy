package ru.unlimmitted.mtwgeasy.controllers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class)

    @ExceptionHandler(IllegalArgumentException)
    ResponseEntity<Map<String, String>> handleInvalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body([error: exception.message])
    }

    @ExceptionHandler(IllegalStateException)
    ResponseEntity<Map<String, String>> handleUnavailable(IllegalStateException exception) {
        log.warn("Request cannot be completed: {}", exception.message)
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body([error: exception.message])
    }

    @ExceptionHandler(RuntimeException)
    ResponseEntity<Map<String, String>> handleRuntimeFailure(RuntimeException exception) {
        log.error("Unexpected API failure", exception)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body([error: "The operation could not be completed"])
    }
}
