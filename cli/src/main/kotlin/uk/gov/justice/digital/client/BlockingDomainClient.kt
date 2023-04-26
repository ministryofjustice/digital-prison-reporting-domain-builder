package uk.gov.justice.digital.client

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpHeaders.ACCEPT
import io.micronaut.http.HttpHeaders.USER_AGENT
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.uri.UriBuilder
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.gov.justice.digital.model.Domain
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpClient.Redirect
import java.net.http.HttpClient.Version
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

interface DomainClient {
    fun getDomains(): Array<Domain>
    fun getDomainWithName(name: String): Domain?
}

/**
 * Blocking client, suitable for CLI use.
 */
@Singleton
class BlockingDomainClient : DomainClient {

    @Inject
    private lateinit var objectMapper: ObjectMapper
    @Inject
    private lateinit var client: HttpClient

    @Value("\${http.client.url}")
    private val baseUrl = "http://localhost:8080"

    private val DOMAIN_RESOURCE = UriBuilder.of("$baseUrl/domain").build()

    override fun getDomains(): Array<Domain> = client.get<Array<Domain>>(DOMAIN_RESOURCE)

    override fun getDomainWithName(name: String): Domain? {
        val requestUri =
            UriBuilder.of(DOMAIN_RESOURCE)
                .queryParam("name", name)
                .build()

        return client.get<Array<Domain>>(requestUri).firstOrNull()
    }

    private inline fun <reified T> HttpClient.get(url: URI): T {
        val request = HttpRequest.newBuilder()
            .GET()
            .headers(
                ACCEPT, APPLICATION_JSON,
                USER_AGENT, "domain-builder-cli/v0.0.1"
            )
            .uri(url)
            .timeout(REQUEST_TIMEOUT)
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

        if (response.statusCode() in 200..299)
            return objectMapper.readValue(response.body(), T::class.java) ?: throw RuntimeException("No data in response")
        else throw RuntimeException("Server returned an error")
    }

    companion object {
        private val REQUEST_TIMEOUT = Duration.ofSeconds(30)
        private val CONNECT_TIMEOUT = Duration.ofSeconds(15)
    }

    @Factory
    private class HttpClientFactory {
        @Singleton
        fun configuredClient(): HttpClient =
            HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(CONNECT_TIMEOUT)
                .build()
    }

}