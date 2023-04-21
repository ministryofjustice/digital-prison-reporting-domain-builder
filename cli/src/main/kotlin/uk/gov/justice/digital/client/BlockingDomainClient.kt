package uk.gov.justice.digital.client

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpHeaders.*
import io.micronaut.http.HttpRequest.GET
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.uri.UriBuilder
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.gov.justice.digital.model.Domain

interface DomainClient {
    fun getDomains(): Array<Domain>
    fun getDomainWithName(name: String): Domain?
}

/**
 * Blocking client, suitable for CLI use.
 */
@Singleton
class BlockingDomainClient : DomainClient {

    @field:Client("\${micronaut.http.services.domain.url}")
    @Inject
    private lateinit var client: HttpClient

    override fun getDomains(): Array<Domain> = client.get<Array<Domain>>(DOMAIN_RESOURCE)

    override fun getDomainWithName(name: String): Domain? {
        val requestUri =
            UriBuilder.of(DOMAIN_RESOURCE)
                .queryParam("name", name)
                .build()
                .toASCIIString()

        return client.get<Array<Domain>>(requestUri).firstOrNull()
    }

    private inline fun <reified T> HttpClient.get(url: String): T =
        this.toBlocking().retrieve(
            GET<String>(url)
                .accept(APPLICATION_JSON)
                .header(USER_AGENT, "domain-builder-cli/v0.0.1"),
            Argument.of(T::class.java)
        )

    companion object {
        private const val DOMAIN_RESOURCE = "/domain"
    }

}