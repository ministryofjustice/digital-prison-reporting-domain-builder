package uk.gov.justice.digital.repository

import jakarta.inject.Singleton
import org.ktorm.database.Database
import org.ktorm.dsl.*
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.repository.table.DomainTable
import java.time.Instant
import java.util.*
import javax.sql.DataSource

@Singleton
class DomainRepository(dataSource: DataSource) {

    // Only connect to the database if we need to
    private val database by lazy { Database.connect(dataSource) }

    fun getDomain(id: UUID): Domain?  {
        return database
            .from(DomainTable)
            .select(DomainTable.data)
            .where(DomainTable.id eq id)
            .map { it[DomainTable.data] }
            .firstOrNull()
    }

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
            val timestamp = Instant.now()
            database
                .insert(DomainTable) {
                    set(it.id, domain.id)
                    set(it.name, domain.name)
                    set(it.data, domain)
                    set(it.created, timestamp)
                    set(it.lastUpdated, timestamp)
                }
        }
        catch (e: Exception) {
            throw CreateFailedException("Failed to create domain with id: ${domain.id} name: ${domain.name}", e)
        }
    }

    fun updateDomain(domain: Domain) {
        database
            .update(DomainTable) {
                set(it.id, domain.id)
                set(it.name, domain.name)
                set(it.data, domain)
                set(it.lastUpdated, Instant.now())
                where { it.id eq domain.id }
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