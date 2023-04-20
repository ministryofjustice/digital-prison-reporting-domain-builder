package uk.gov.justice.digital.service

import jakarta.inject.Singleton
import uk.gov.justice.digital.model.Domain

/**
 * Initial service that will handle calls to the backend API and processing the results.
 *
 * For now this just provides hardcoded data to support the development of the CLI.
 *
 * See DPR-363 which covers the work to integrate the CLI with the backend API.
 */
@Singleton
class DomainService {

    // TODO - introduce client dependency
    private val domains = emptyList<Domain>()

    fun getAllDomains(): List<Domain> {
        return domains
    }

    fun getDomainWithName(name: String): Domain? {
        return domains.firstOrNull { it.name == name }
    }

}
