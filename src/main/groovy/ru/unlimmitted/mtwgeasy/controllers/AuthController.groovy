package ru.unlimmitted.mtwgeasy.controllers

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController {

	@PostMapping("/login")
	def login(@RequestParam("username") String username, @RequestParam("password") String password, HttpServletResponse response) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication()
		return ResponseEntity.ok().body(auth)
	}

	@GetMapping("/status")
	def getStatus(HttpServletResponse response) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication()

		if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != "anonymousUser") {
			response.setStatus(HttpServletResponse.SC_OK)
			return [authenticated: auth.authenticated, user: auth.getName()]
		} else {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
			return [authenticated: auth.authenticated]
		}
	}
}
