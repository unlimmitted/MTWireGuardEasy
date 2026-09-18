package ru.unlimmitted.mtwgeasy.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import ru.unlimmitted.mtwgeasy.services.CustomAuthenticationProvider

@Configuration
class SecurityConfig(
    private val customAuthenticationProvider: CustomAuthenticationProvider,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse()
        val csrfRequestHandler = CsrfTokenRequestAttributeHandler()
        return http
            .authenticationProvider(customAuthenticationProvider)
            .csrf { csrf ->
                csrf
                    .csrfTokenRepository(csrfRepository)
                    .csrfTokenRequestHandler(csrfRequestHandler)
            }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/js/**", "/css/**", "/logo.png", "/favicon.png", "/FeatureMono.ttf")
                    .permitAll()
                    .requestMatchers("/login", "/auth/csrf")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .exceptionHandling { exceptions ->
                val unauthorized = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                exceptions
                    .defaultAuthenticationEntryPointFor(unauthorized, AntPathRequestMatcher("/api/**"))
                    .defaultAuthenticationEntryPointFor(unauthorized, AntPathRequestMatcher("/ws/**"))
                    .defaultAuthenticationEntryPointFor(unauthorized, AntPathRequestMatcher("/auth/status"))
            }
            .formLogin { form ->
                form
                    .loginPage("/login")
                    .loginProcessingUrl("/auth/login")
                    .defaultSuccessUrl("/", true)
                    .permitAll()
            }
            .logout { logout ->
                logout
                    .logoutUrl("/auth/logout")
                    .logoutSuccessUrl("/login")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
            }
            .build()
    }

    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager =
        authConfig.authenticationManager
}
