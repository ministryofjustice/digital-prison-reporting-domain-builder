package uk.gov.justice.digital.cli.service

import jakarta.inject.Singleton
import uk.gov.justice.digital.cli.client.DomainClient
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status

/**
 * Service that wraps calls to the BlockingDomainClient which is responsible for interacting with the backend REST API.
 *
 * Applies any business logic to the client responses where relevant.
 */
@Singleton
class DomainService(private val client: DomainClient) {
    fun getAllDomains(): Array<Domain> = client.getDomains()
    fun getDomains(name: String, status: Status? = null): Array<Domain> = client.getDomains(name, status)
}
