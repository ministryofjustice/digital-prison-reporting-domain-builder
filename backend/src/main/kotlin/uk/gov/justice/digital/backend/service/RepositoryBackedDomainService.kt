package uk.gov.justice.digital.backend.service

import jakarta.inject.Singleton
import uk.gov.justice.digital.backend.client.domain.DomainRegistryClient
import uk.gov.justice.digital.backend.repository.DomainRepository
import uk.gov.justice.digital.backend.validator.InvalidSparkSqlResult
import uk.gov.justice.digital.backend.validator.SparkSqlValidator
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.model.Status.DRAFT
import uk.gov.justice.digital.model.Status.PUBLISHED
import uk.gov.justice.digital.model.Table
import uk.gov.justice.digital.model.WriteableDomain
import java.util.*

/**
 * Service that will handle calls to the backend database.
 *
 * Supports creation and retrieval of domains delegating to the repository.
 *
 * Update and delete to follow in later work.
 */
@Singleton
class RepositoryBackedDomainService(private val repository: DomainRepository,
                                    private val sqlValidator: SparkSqlValidator,
                                    private val domainRegistryClient: DomainRegistryClient): DomainService {

    // TODO - review this usage - do we ever get a status to pass to the repository?
    override fun getDomains(name: String?, status: Status?): List<Domain> = repository.getDomains(name)

    override fun getDomain(id: UUID): Domain? = repository.getDomain(id)

    override fun createDomain(domain: WriteableDomain): UUID =
        domain
            .validateSql { it.mapping?.viewText }
            .validateSql { it.transform.viewText }
            .let { repository.createDomain(it) }

    override fun publishDomain(name: String, status: Status): UUID {
        if (status.canBePublished()) {
            val domains = repository.getDomains(name, status)

            if (domains.size == 1) {
                val domain = domains.first().copy(status = PUBLISHED)

                repository.withinTransaction {
                    if (status == DRAFT) {
                        val existingDomain = repository.getDomains(name, PUBLISHED).firstOrNull()
                        existingDomain?.let { repository.deleteDomain(it.id) }
                        repository.updateDomain(domain)
                    } else {
                        repository.updateDomain(domain)
                    }

                    domainRegistryClient.publish(domain)
                }

                return domain.id
            } else throw RuntimeException("Expected one domain with name: $name status: $status")
        }
        else throw InvalidStatusException("Failed to publish domain: $name Invalid status: $status")
    }

    private fun validateDomainSql(domain: WriteableDomain, getSqlFromTable: (Table) -> String?): WriteableDomain =
        domain.tables
            ?.mapNotNull(getSqlFromTable)
            ?.map { sqlValidator.validate(it) }
            ?.filterIsInstance<InvalidSparkSqlResult>()
            ?.firstOrNull()
            ?.let { throw InvalidSparkSqlException(it) } ?: domain

    private fun WriteableDomain.validateSql(getSqlFromTable: (Table) -> String?): WriteableDomain =
        validateDomainSql(this, getSqlFromTable)

    private fun Status.canBePublished() = this == Status.DRAFT
}

class InvalidSparkSqlException(validationResult: InvalidSparkSqlResult) : RuntimeException(validationResult.reason)
class InvalidStatusException(message: String) : RuntimeException(message)