package uk.gov.justice.digital.cli.client

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import io.micronaut.core.type.Argument
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
    fun getDomains(): List<Domain>
    fun getDomains(name: String, status: Status? = null): List<Domain>
    fun createDomain(domain: WriteableDomain): String
    fun previewDomain(name: String, status: Status, limit: Int): List<List<String?>>
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

    private val previewResource by lazy {
        UriBuilder.of("$baseUrl/preview").build()
    }

    override fun getDomains(): List<Domain> = client.get(domainResource, Argument.listOf(Domain::class.java))

    override fun getDomains(name: String, status: Status?): List<Domain> {
        val requestUri =
            UriBuilder.of(domainResource)
                .queryParam("name", name)
                .withOptionalParameter("status", status?.name)
                .build()

        return client.get(requestUri, Argument.listOf(Domain::class.java))
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

    private inline fun <reified T> HttpClient.get(uri: URI, type: Argument<T>): T {
        val request = configuredRequestBuilder(uri)
            .GET()
            .build()

        val response = this.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() in 200..299)
            return response.deserialize(type)
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

    private inline fun <reified T> HttpResponse<String>.deserialize(type: Argument<T>): T =
        this.body()
            ?.let { objectMapper.readValue(it, type) }
            ?: throw UnexpectedResponseException("No data in response")

    override fun previewDomain(name: String, status: Status, limit: Int): List<List<String?>> {
        val requestBody = mapOf(
            "domainName" to name,
            "status" to status,
            "limit" to limit
        )
        val request = configuredRequestBuilder(previewResource)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
            .build()

        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() == 200) return response.deserialize(Argument.listOf(Argument.LIST_OF_STRING))
        else if (response.statusCode() == 404) throw DomainNotFoundException("Domain with name: $name and status: $status was not found")
        else throw UnexpectedResponseException("Server returned an unexpected response: HTTP ${response.statusCode()}")
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
class DomainNotFoundException(message: String) : ClientException(message)
class UnexpectedResponseException(message: String) : ClientException(message)
