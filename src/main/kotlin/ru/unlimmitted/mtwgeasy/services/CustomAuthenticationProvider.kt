package ru.unlimmitted.mtwgeasy.services

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationProvider : AuthenticationProvider {
    private val envUser = System.getenv("MIKROTIK_USER")
    private val envPassword = System.getenv("MIKROTIK_PASSWORD")

    override fun authenticate(authentication: Authentication): Authentication {
        val username = authentication.name
        val password = authentication.credentials.toString()
        if (!envUser.isNullOrEmpty() && !envPassword.isNullOrEmpty() && username == envUser && password == envPassword) {
            val authorities = emptyList<org.springframework.security.core.GrantedAuthority>()
            val userDetails = User(username, "", authorities)
            return UsernamePasswordAuthenticationToken(userDetails, null, authorities)
        }
        throw BadCredentialsException("Invalid credentials")
    }

    override fun supports(authentication: Class<*>): Boolean =
        UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication)
}
