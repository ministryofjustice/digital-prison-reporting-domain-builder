package uk.gov.justice.digital.cli.client

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpHeaders.*
import io.micronaut.http.HttpStatus
import io.micronaut.http.HttpStatus.*
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.hateoas.JsonError
import io.micronaut.http.uri.UriBuilder
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.gov.justice.digital.headers.ApiKeyHeader
import uk.gov.justice.digital.headers.Header
import uk.gov.justice.digital.headers.SessionIdHeader
import uk.gov.justice.digital.headers.TraceIdHeader
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.model.WriteableDomain
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpClient.Redirect
import java.net.http.HttpClient.Version
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*

interface DomainClient {
    fun getDomains(): Array<Domain>
    fun getDomains(name: String, status: Status? = null): Array<Domain>
    fun createDomain(domain: WriteableDomain): String
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
    private lateinit var baseUrl: String

    @Value("\${http.client.apiKey}")
    private lateinit var apiKey: String

    private val domainResource by lazy {
        UriBuilder.of("$baseUrl/domain").build()
    }

    override fun getDomains(): Array<Domain> = client.get<Array<Domain>>(domainResource)

    override fun getDomains(name: String, status: Status?): Array<Domain> {
        val requestUri =
            UriBuilder.of(domainResource)
                .queryParam("name", name)
                .withOptionalParameter("status", status?.name)
                .build()

        return client.get<Array<Domain>>(requestUri)
    }

    // Only add the query parameter if the value is not null.
    private fun UriBuilder.withOptionalParameter(name: String, value: String?): UriBuilder {
        return value?.let { this.queryParam(name, it) } ?: this
    }

    private fun configuredRequestBuilder(uri: URI) =
        HttpRequest.newBuilder(uri)
            .header(ACCEPT, APPLICATION_JSON)
            .header(USER_AGENT, "domain-builder-cli/v0.0.1")
            .withCustomHeader(TraceIdHeader())
            .withCustomHeader(SessionIdHeader.instance)
            .withCustomHeader(ApiKeyHeader(apiKey))
            .timeout(REQUEST_TIMEOUT)

    private fun HttpRequest.Builder.withCustomHeader(header: Header) = this.header(header.name, header.value)

    private inline fun <reified T> HttpClient.get(uri: URI): T {
        val request = configuredRequestBuilder(uri)
            .GET()
            .build()

        val response = this.send(request, HttpResponse.BodyHandlers.ofInputStream())

        if (response.statusCode() in 200..299)
            return objectMapper.readValue(response.body(), T::class.java) ?: throw UnexpectedResponseException("No data in response")
        else throw UnexpectedResponseException("Server returned an unexpected response: HTTP ${response.statusCode()}")
    }

    override fun createDomain(domain: WriteableDomain): String {
        val request = configuredRequestBuilder(domainResource)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(domain)))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        return when(HttpStatus.valueOf(response.statusCode())) {
            CREATED -> response.headers()
                .firstValue(LOCATION)
                .orElseThrow { IllegalStateException("No $LOCATION header on response") }
            CONFLICT -> throw ConflictException("Domain with name: ${domain.name} and status: ${domain.status} already exists")
            BAD_REQUEST -> throw BadRequestException(createErrorMessageForBadRequest(response))
            else -> throw UnexpectedResponseException("Got unexpected response from server: $response")
        }
    }

    private fun <T> createErrorMessageForBadRequest(response: HttpResponse<T>) = response.headers()
            .firstValue(CONTENT_TYPE)
            .filter { it.equals(APPLICATION_JSON) }
            .flatMap { _ ->
                try {
                    val jsonError = objectMapper.readValue(response.body().toString(), JsonError::class.java)
                    Optional.ofNullable<String>(jsonError?.message)
                }
                catch (ex: Exception) { Optional.empty() }
            }
            .map { "The server could not process your request because:\n$it" }
            .orElse("The server could not process your request")

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

sealed class ClientException(message: String) : RuntimeException(message)
class BadRequestException(message: String) : ClientException(message)
class ConflictException(message: String) : ClientException(message)
class UnexpectedResponseException(message: String) : ClientException(message)
