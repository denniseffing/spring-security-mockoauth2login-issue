package com.example.demo

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOAuth2Login
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.util.context.ContextView

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["gateway.target.uri.some-route=http://localhost:\${wiremock.server.port}"]
)
@AutoConfigureWireMock(
    port = 0,
    stubs = ["file:src/test/resources/mappings"],
    files = ["file:src/test/resources"]
)
@AutoConfigureWebTestClient
class SecurityConfigurationIntTestWithWorkaround {

    @Autowired
    private lateinit var repo: ReactiveClientRegistrationRepository

    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    fun requestsWithAuthenticatedOauth2UserShouldBeOk() {
        val webClientWithOAuth = webClient.mutateWith(
            mockOAuth2Login()
                .attributes { map -> map["sub"] = "default" }
                .clientRegistration(repo.findByRegistrationId("keycloak").block())
        )
        webClientWithOAuth.get().uri("/route/").exchange()
            .expectStatus().isOk()
    }

    @TestConfiguration
    internal class MockOAuth2LoginConfiguration {
        @Bean
        fun authorizedClientManager(): ReactiveOAuth2AuthorizedClientManager {
            return TestReactiveOAuth2AuthorizedClientManager()
        }
    }


    internal class TestReactiveOAuth2AuthorizedClientManager : ReactiveOAuth2AuthorizedClientManager {
        override fun authorize(authorizeRequest: OAuth2AuthorizeRequest): Mono<OAuth2AuthorizedClient> =
            Mono.justOrEmpty(authorizeRequest.getAttribute<ServerWebExchange>(ServerWebExchange::class.java.name))
                .switchIfEmpty(currentServerWebExchangeMono)
                .mapNotNull { exchange: ServerWebExchange ->
                    exchange.getAttribute(
                        TOKEN_ATTR_NAME
                    )
                }

        companion object {
            const val TOKEN_ATTR_NAME =
                "org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers\$OAuth2ClientMutator\$TestReactiveOAuth2AuthorizedClientManager.TOKEN"

            private val currentServerWebExchangeMono =
                Mono.deferContextual { data -> Mono.just(data) }
                    .filter { c -> c!!.hasKey(ServerWebExchange::class.java) }
                    .map { c -> c!!.get(ServerWebExchange::class.java) }
        }
    }

}
