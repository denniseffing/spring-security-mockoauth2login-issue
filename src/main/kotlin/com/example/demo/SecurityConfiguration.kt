package com.example.demo

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity.AuthorizeExchangeSpec
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain

@EnableWebFluxSecurity
@Configuration
class SecurityConfiguration {

    @Bean
    @Throws(Exception::class)
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http.csrf()
            .disable()
            .authorizeExchange { authorize: AuthorizeExchangeSpec ->
                authorize
                    .pathMatchers("/route/**")
                    .hasAuthority("OAUTH2_USER")
            }.oauth2Login()
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder()
    }
}
