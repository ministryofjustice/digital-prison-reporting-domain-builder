package uk.gov.justice.digital.backend.service

import jakarta.inject.Singleton
import uk.gov.justice.digital.backend.repository.DomainRepository
import uk.gov.justice.digital.backend.validator.InvalidSparkSql
import uk.gov.justice.digital.backend.validator.SparkSqlValidator
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.model.WriteableDomain
import java.lang.RuntimeException
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
class RepositoryBackedDomainService(
    private val repository: DomainRepository,
    private val sqlValidator: SparkSqlValidator): DomainService {

    override fun getDomains(name: String?, status: Status?): List<Domain> = repository.getDomains(name)

    override fun getDomain(id: UUID): Domain? = repository.getDomain(id)

    override fun createDomain(domain: WriteableDomain): UUID {
        // Map list of tables and validate each mapping so we get a list of validation results
        val results = domain.tables
            .map { it.mapping.viewText }
            .map { sqlValidator.validate(it) }
            .filterNot { it.isValid }

        if (results.isEmpty()) return repository.createDomain(domain)
        // TODO - tidy this up
        else throw RuntimeException((results.first() as InvalidSparkSql).reason)
    }
}
