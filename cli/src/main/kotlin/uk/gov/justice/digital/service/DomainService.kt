package uk.gov.justice.digital.service

import jakarta.inject.Singleton
import uk.gov.justice.digital.client.DomainClient
import uk.gov.justice.digital.model.Domain

/**
 * Service that wraps calls to the BlockingDomainClient which is responsible for interacting with the backend REST API.
 *
 * Applies any business logic to the client responses where relevant.
 */
@Singleton
class DomainService(private val client: DomainClient) {

    fun getAllDomains(): List<Domain> = client.getDomains().toList()

    fun getDomainWithName(name: String): Domain? = client.getDomainWithName(name)

}
