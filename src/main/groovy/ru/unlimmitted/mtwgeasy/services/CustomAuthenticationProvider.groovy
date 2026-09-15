package ru.unlimmitted.mtwgeasy.services


import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationProvider implements AuthenticationProvider {

	String envUser = System.getenv("MIKROTIK_USER")

	String envPassword = System.getenv("MIKROTIK_PASSWORD")

	@Override
	Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = authentication.getName()
		String password = authentication.getCredentials().toString()

		if (envUser && envPassword && username == envUser && password == envPassword) {
			List<GrantedAuthority> authorities = []
			UserDetails userDetails = new User(username, "", authorities)
			return new UsernamePasswordAuthenticationToken(userDetails, null, authorities)
		} else {
			throw new BadCredentialsException("Invalid credentials")
		}
	}

	@Override
	boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication)
	}
}
