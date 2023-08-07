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

    // TODO - this needs to
    //      o add the domain in published state to the dynamodb table replacing any existing domain
    //      o store domain source relationships to support lookups
    //      o store a published version of the domain in postgres
    override fun publishDomain(name: String, status: Status): UUID {
        // TODO - We should only get one domain here - fail if not
        val domains = repository.getDomains(name, status)

        if (domains.size == 1) {

            val domain = domains.first().copy(status = PUBLISHED)

            // KTORM - can we do this in one transaction?
            // If we're promoting a draft which is usually the case, removing the PUBLISHED record if it exists.
            if (status == DRAFT) {
                val existingDomain = repository.getDomains(name, PUBLISHED).firstOrNull()
                existingDomain?.let { repository.deleteDomain(it.id) }
                repository.updateDomain(domain)
            }
            else {
                repository.updateDomain(domain)
            }

            // TODO - update dynamo
            return domain.id
        }
        else throw RuntimeException("Expected one domain with name: $name status: $status")

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

}

class InvalidSparkSqlException(validationResult: InvalidSparkSqlResult) : RuntimeException(validationResult.reason)