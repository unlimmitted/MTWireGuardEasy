package ru.unlimmitted.mtwgeasy.controllers

import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController {
    @GetMapping("/csrf")
    fun csrf(token: CsrfToken): Map<String, String> = mapOf("token" to token.token)

    @GetMapping("/status")
    fun getStatus(response: HttpServletResponse): Map<String, Any> {
        val auth = SecurityContextHolder.getContext().authentication
        return if (auth != null && auth.isAuthenticated && auth.principal != "anonymousUser") {
            response.status = HttpServletResponse.SC_OK
            mapOf("authenticated" to auth.isAuthenticated, "user" to auth.name)
        } else {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            mapOf("authenticated" to false)
        }
    }
}
