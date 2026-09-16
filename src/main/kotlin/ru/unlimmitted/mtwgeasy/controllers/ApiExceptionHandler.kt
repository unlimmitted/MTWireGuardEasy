package ru.unlimmitted.mtwgeasy.controllers

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleInvalidRequest(exception: IllegalArgumentException): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to (exception.message ?: "Invalid request")))

    @ExceptionHandler(IllegalStateException::class)
    fun handleUnavailable(exception: IllegalStateException): ResponseEntity<Map<String, String>> {
        log.warn("Request cannot be completed: {}", exception.message)
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(mapOf("error" to (exception.message ?: "Service unavailable")))
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeFailure(exception: RuntimeException): ResponseEntity<Map<String, String>> {
        log.error("Unexpected API failure", exception)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to "The operation could not be completed"))
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    }
}
