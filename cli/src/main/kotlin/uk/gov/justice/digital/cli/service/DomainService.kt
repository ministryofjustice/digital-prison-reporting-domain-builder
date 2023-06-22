package uk.gov.justice.digital.cli.service

import io.micronaut.serde.ObjectMapper
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.gov.justice.digital.cli.client.DomainClient
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.model.WriteableDomain

/**
 * Service that wraps calls to the BlockingDomainClient which is responsible for interacting with the backend REST API.
 *
 * Applies any business logic to the client responses where relevant.
 */
@Singleton
class DomainService(private val client: DomainClient) {

    @Inject
    private lateinit var objectMapper: ObjectMapper

    fun getAllDomains(): Array<Domain> = client.getDomains()

    fun getDomains(name: String, status: Status? = null): Array<Domain> = client.getDomains(name, status)

    fun createDomain(domain: String): String {
        val writeableDomain = objectMapper.readValue(domain, WriteableDomain::class.java)
        return client.createDomain(writeableDomain!!)
    }
}
