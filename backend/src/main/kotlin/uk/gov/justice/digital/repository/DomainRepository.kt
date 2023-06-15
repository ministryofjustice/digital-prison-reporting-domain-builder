package uk.gov.justice.digital.repository

import jakarta.inject.Singleton
import org.ktorm.database.Database
import org.ktorm.dsl.*
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.repository.table.DomainTable
import uk.gov.justice.digital.time.ClockProvider
import java.util.*
import javax.sql.DataSource

@Singleton
class DomainRepository(dataSource: DataSource, clockProvider: ClockProvider) {

    // Only connect to the database if we need to
    private val database by lazy { Database.connect(dataSource) }

    private val clock = clockProvider.clock

    fun getDomain(id: UUID): Domain?  {
        return database
            .from(DomainTable)
            .select(DomainTable.data)
            .where(DomainTable.id eq id)
            .map { it[DomainTable.data] }
            .firstOrNull()
    }

    // TODO - this needs to support state too
    fun getDomains(name: String? = null): List<Domain> {
        return database
            .from(DomainTable)
            .select(DomainTable.data)
            .whereWithConditions { conditions ->
                name?.let { conditions += DomainTable.name eq it }
            }
            .map { it[DomainTable.data] }
            .filterNotNull()
    }

    fun createDomain(domain: Domain) {
        try {
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
        }
        catch (e: Exception) {
            throw CreateFailedException("Failed to create domain with id: ${domain.id} name: ${domain.name} status: ${domain.status}", e)
        }
    }

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
class UpdateFailedException(message: String) : RepositoryException(message)
class DeleteFailedException(message: String) : RepositoryException(message)