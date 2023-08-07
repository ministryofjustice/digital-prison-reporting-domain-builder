package uk.gov.justice.digital.backend.repository

import jakarta.inject.Singleton
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.backend.repository.table.DomainTable
import uk.gov.justice.digital.model.WriteableDomain
import uk.gov.justice.digital.time.ClockProvider
import java.util.*
import javax.sql.DataSource

@Singleton
class DomainRepository(dataSource: DataSource, clockProvider: ClockProvider) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    // Only connect to the database if we need to
    private val database by lazy { Database.connect(dataSource) }

    private val clock = clockProvider.clock

    fun getDomain(id: UUID): Domain? {
        logger.info("Fetching domain with ID: {}", id)

        val result = database
            .from(DomainTable)
            .select(DomainTable.data)
            .where(DomainTable.id eq id)
            .map { it[DomainTable.data] }
            .firstOrNull()

        logger.info("Domain with ID: {} {}", id, result?.let { "found" } ?: "not found" )

        return result
    }

    fun getDomains(name: String? = null, status: Status? = null): List<Domain> {
        logger.info("Fetching domains with name: {} status: {}", name, status)

        val result = database
            .from(DomainTable)
            .select(DomainTable.data)
            .whereWithConditions { conditions ->
                name?.let { conditions += DomainTable.name eq it }
                status?.let { conditions += DomainTable.status eq it }
            }
            .map { it[DomainTable.data] }
            .filterNotNull()

        logger.info("Returning {} matching domains", result.size)

        return result
    }

    fun createDomain(writeableDomain: WriteableDomain): UUID = createDomain(writeableDomain.toDomain())

    private fun WriteableDomain.toDomain() =
        Domain(
            UUID.randomUUID(),
            name,
            description,
            version,
            location,
            tags,
            owner,
            author,
            tables,
        )

    fun createDomain(domain: Domain): UUID {
        try {
            logger.info("Creating domain with name: {} status: {}", domain.name, domain.status)

            val now = clock.instant()
            // Set optional values on the domain where no value is set.
            val domainForInsert = domain.copy(
                    created = domain.created ?: now,
                    lastUpdated = domain.lastUpdated ?: now,
            )
            database
                .insert(DomainTable) {
                    set(it.id, domain.id)
                    set(it.name, domain.name)
                    set(it.status, domain.status)
                    set(it.data, domainForInsert)
                    set(it.created, domainForInsert.created)
                    set(it.lastUpdated, domainForInsert.lastUpdated)
                }

            logger.info("Successfully created domain with id: ${domain.id}")
            return domain.id
        }
        catch (px: PSQLException) {
            if (px.isUniqueConstraintViolation()) throw DuplicateKeyException(domain.exceptionMessage(), px)
            logger.error(domain.exceptionMessage(), px)
            throw CreateFailedException(domain.exceptionMessage(), px)
        }
        catch (e: Exception) {
            logger.error(domain.exceptionMessage(), e)
            throw CreateFailedException(domain.exceptionMessage(), e)
        }
    }

    private fun PSQLException.isUniqueConstraintViolation() = PSQLState.UNIQUE_VIOLATION.state.equals(this.sqlState)

    private fun Domain.exceptionMessage() = "Failed to create domain with id: $id name: $name status: $status"

    fun updateDomain(domain: Domain) {
        val now = clock.instant()
        // Set optional values on the domain where no value is set.
        val updatedDomain = domain.copy(lastUpdated = domain.lastUpdated ?: now)
        database
            .update(DomainTable) {
                set(it.id, updatedDomain.id)
                set(it.name, updatedDomain.name)
                set(it.status, updatedDomain.status)
                set(it.data, updatedDomain)
                set(it.lastUpdated, updatedDomain.lastUpdated)
                where { it.id eq updatedDomain.id }
            }
            .throwExceptionIfNoRecordsChanged(UpdateFailedException("Failed to update domain with id: ${domain.id}"))
    }

    fun deleteDomain(id: UUID) {
        database
            .delete(DomainTable) { it.id eq id }
            .throwExceptionIfNoRecordsChanged(DeleteFailedException("Failed to delete domain with id: $id"))
    }

    private fun Int.throwExceptionIfNoRecordsChanged(e: RepositoryException) {
        if (this == 0) throw e
    }

}

sealed class RepositoryException(message: String, cause: Exception? = null) : RuntimeException(message, cause)
class CreateFailedException(message: String, cause: Exception) : RepositoryException(message, cause)
class DuplicateKeyException(message: String, cause: Exception) : RepositoryException(message, cause)
class UpdateFailedException(message: String) : RepositoryException(message)
class DeleteFailedException(message: String) : RepositoryException(message)