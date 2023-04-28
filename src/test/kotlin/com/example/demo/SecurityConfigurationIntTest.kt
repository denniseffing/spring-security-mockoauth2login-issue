package com.example.demo

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOAuth2Login
import org.springframework.test.web.reactive.server.WebTestClient

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
class SecurityConfigurationIntTest {

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
        webClientWithOAuth.get().uri("/route/subroute").exchange()
            .expectStatus().isOk()
    }
}
