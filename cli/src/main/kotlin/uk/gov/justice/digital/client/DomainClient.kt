package uk.gov.justice.digital.client

import io.micronaut.http.HttpRequest.GET
import io.micronaut.http.client.annotation.Client
import io.micronaut.reactor.http.client.ReactorHttpClient
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import uk.gov.justice.digital.model.Domain


class DomainClient {

    @Client("domain")
    @Inject
    private lateinit var client: ReactorHttpClient

    // TODO -review types here
    fun getDomains(): List<Domain> =
        Flux.from<List<Domain>>(client.retrieve(GET("/domain"), List::class))

    fun getDomainWithName(name: String): Domain? =
        Flux.from<Domain>(client.retrieve(GET("/domain?$name"), Domain::class))

}