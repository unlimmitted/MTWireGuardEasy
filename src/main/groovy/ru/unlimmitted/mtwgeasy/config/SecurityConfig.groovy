package ru.unlimmitted.mtwgeasy.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import ru.unlimmitted.mtwgeasy.services.CustomAuthenticationProvider

@Configuration
class SecurityConfig {

	private final CustomAuthenticationProvider customAuthenticationProvider

	SecurityConfig(CustomAuthenticationProvider customAuthenticationProvider) {
		this.customAuthenticationProvider = customAuthenticationProvider
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		HttpSessionRequestCache requestCache = new HttpSessionRequestCache()
		return http
				.csrf().disable()
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/js/**", "/css/**", "/logo.png", "/favicon.png").permitAll()
						.requestMatchers("/login").permitAll()
						.anyRequest().authenticated()
				)
				.formLogin(form -> form
						.loginPage("/login")
						.loginProcessingUrl("/auth/login")
						.defaultSuccessUrl("/", true)
						.permitAll()
				)
				.logout(logout -> logout
						.logoutUrl("/auth/logout")
						.logoutSuccessUrl("/login")
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID")
						.permitAll()
				)
				.build()
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager()
	}

	@Bean
	static NoOpPasswordEncoder passwordEncoder() {
		return NoOpPasswordEncoder.getInstance()
	}
}
