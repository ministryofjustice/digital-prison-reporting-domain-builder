package uk.gov.justice.digital.service

import jakarta.inject.Singleton
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.repository.DomainRepository
import java.util.*

/**
 * Service that will handle calls to the backend database.
 *
 * For now this just supports the retrieval of domains but will be extended in future work to cover creation,
 * updating and deletion.
 */
@Singleton
class DomainService(private val repository: DomainRepository) {

    fun getDomains(name: String? = null): List<Domain> = repository.getDomains(name)

    fun getDomain(id: UUID): Domain? = repository.getDomain(id)

}
