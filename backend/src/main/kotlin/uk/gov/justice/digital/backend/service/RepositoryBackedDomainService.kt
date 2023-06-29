package uk.gov.justice.digital.backend.service

import jakarta.inject.Singleton
import uk.gov.justice.digital.backend.repository.DomainRepository
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.model.WriteableDomain
import java.util.*

interface DomainService {
    fun getDomains(name: String? = null, status: Status? = null): List<Domain>
    fun getDomain(id: UUID): Domain?
    fun createDomain(domain: WriteableDomain): UUID
}

/**
 * Service that will handle calls to the backend database.
 *
 * Supports creation and retrieval of domains delegating to the repository.
 *
 * Update and delete to follow in later work.
 */
@Singleton
class RepositoryBackedDomainService(private val repository: DomainRepository): DomainService {
    override fun getDomains(name: String?, status: Status?): List<Domain> = repository.getDomains(name)
    override fun getDomain(id: UUID): Domain? = repository.getDomain(id)
    override fun createDomain(domain: WriteableDomain): UUID = repository.createDomain(domain)
}
