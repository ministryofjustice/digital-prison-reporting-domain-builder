package uk.gov.justice.digital.service

import jakarta.inject.Singleton
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.repository.DomainRepository
import java.util.*

interface DomainService {
    fun getDomains(name: String? = null, status: Status? = null): List<Domain>
    fun getDomain(id: UUID): Domain?
}

/**
 * Service that will handle calls to the backend database.
 *
 * For now this just supports the retrieval of domains but will be extended in future work to cover creation,
 * updating and deletion.
 */
@Singleton
class RepositoryBackedDomainService(private val repository: DomainRepository): DomainService {
    override fun getDomains(name: String?, status: Status?): List<Domain> = repository.getDomains(name)
    override fun getDomain(id: UUID): Domain? = repository.getDomain(id)
}
