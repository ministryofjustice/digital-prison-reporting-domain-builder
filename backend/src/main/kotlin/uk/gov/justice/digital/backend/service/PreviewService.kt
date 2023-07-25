package uk.gov.justice.digital.backend.service

import jakarta.inject.Singleton
import uk.gov.justice.digital.backend.client.preview.PreviewClient
import uk.gov.justice.digital.backend.converter.DomainToPreviewQueryConverter
import uk.gov.justice.digital.backend.repository.DomainRepository
import uk.gov.justice.digital.model.Status
import kotlin.math.min

@Singleton
class PreviewService(private val client: PreviewClient,
                     private val repository: DomainRepository,
                     private val converter: DomainToPreviewQueryConverter) {

    fun preview(domainName: String, status: Status?, limit: Int): List<Map<String, String>> {
        val domains = repository.getDomains(domainName, status)

        val domain = if (domains.isEmpty())
            throw DomainNotFoundException("No domains found for name: $domainName status: $status")
        else if (domains.size > 1)
            throw MultipleDomainsFoundException("More than one matching domain for name: $domainName status: $status")
        else domains.first()

        // TODO - right now we only handle single table domains so we just preview the first table where present.
        return domain.tables
            ?.firstOrNull()
            ?.let { preview(it.transform.viewText, limit )}
            ?: throw NoTablesInDomainException("No tables found in domain with name: $domainName status: $status")
    }

    private fun preview(sql: String, limit: Int): List<Map<String, String>> {
        val convertedQuery = converter.convertQuery(sql, min(limit, MaximumLimit))
        return client.runQuery(convertedQuery)
    }

    companion object {
        const val MaximumLimit = 100 // TODO - make this configurable?
    }

}

sealed class PreviewServiceException(message: String, cause: Exception? = null) : RuntimeException(message, cause)
class DomainNotFoundException(message: String) : PreviewServiceException(message)
class MultipleDomainsFoundException(message: String) : PreviewServiceException(message)
class NoTablesInDomainException(message: String) : PreviewServiceException(message)