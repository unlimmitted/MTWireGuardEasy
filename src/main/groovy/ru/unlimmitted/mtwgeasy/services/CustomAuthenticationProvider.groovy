package ru.unlimmitted.mtwgeasy.services


import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationProvider implements AuthenticationProvider {

	String envUser = System.getenv("MIKROTIK_USER")

	String envPassword = System.getenv("MIKROTIK_PASSWORD")

	@Override
	Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = authentication.getName()
		String password = authentication.getCredentials().toString()

		if (username == envUser && password == envPassword) {
			List<GrantedAuthority> authorities = []
			UserDetails userDetails = new User(username, password, authorities)
			return new UsernamePasswordAuthenticationToken(userDetails, password, authorities)
		} else {
			return null
		}
	}

	@Override
	boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication)
	}
}