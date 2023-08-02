package uk.gov.justice.digital.cli.service

import com.fasterxml.jackson.core.JsonParseException
import io.micronaut.serde.ObjectMapper
import io.micronaut.serde.exceptions.SerdeException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.gov.justice.digital.cli.client.DomainClient
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.model.WriteableDomain
import java.lang.RuntimeException

/**
 * Service that wraps calls to the BlockingDomainClient which is responsible for interacting with the backend REST API.
 *
 * Applies any business logic to the client responses where relevant.
 */
@Singleton
class DomainService(private val client: DomainClient) {

    @Inject
    private lateinit var objectMapper: ObjectMapper

    fun getAllDomains(): List<Domain> = client.getDomains()

    fun getDomains(name: String, status: Status? = null): List<Domain> = client.getDomains(name, status)

    fun createDomain(domain: WriteableDomain): String = client.createDomain(domain)

    fun createDomain(domain: String): String {
        try {
            val writeableDomain = objectMapper.readValue(domain, WriteableDomain::class.java)
            return createDomain(writeableDomain!!)
        }
        catch (jpx: JsonParseException) {
            val location = jpx.location
            throw JsonParsingFailedException(
                "${jpx.originalMessage} on line: ${location.lineNr} at column: ${location.columnNr}",
                jpx
            )
        }
        catch (sdx: SerdeException) {
            throw JsonParsingFailedException("Failed to parse json: ${sdx.message}", sdx)
        }
    }

    fun previewDomain(name: String, status: Status, limit: Int): List<List<String?>> =
        client.previewDomain(name, status, limit)

}

sealed class DomainServiceException(message: String, cause: Exception? = null) : RuntimeException(message, cause)
class JsonParsingFailedException(message: String, cause: Exception) : DomainServiceException(message, cause)