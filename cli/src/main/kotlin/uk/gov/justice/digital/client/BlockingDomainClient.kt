package uk.gov.justice.digital.client

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpHeaders.*
import io.micronaut.http.HttpRequest.GET
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.gov.justice.digital.model.Domain

// Blocking client - suitable for cli use

@Singleton
class BlockingDomainClient {

    @field:Client("\${micronaut.http.services.domain.url}")
    @Inject
    private lateinit var client: HttpClient

    fun getDomains(): Array<Domain> = client.get<Array<Domain>>("/domain")

    // TODO - use url to build urls
    fun getDomainWithName(name: String): Domain? = client.get<Array<Domain>>("/domain?$name").firstOrNull()

    private inline fun <reified T> HttpClient.get(url: String): T =
        client.toBlocking().retrieve(
            GET<String>(url)
                .accept(APPLICATION_JSON)
                .header(USER_AGENT, "domain-builder-cli/v0.0.1"),
            Argument.of(T::class.java)
        )

}