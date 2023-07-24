package uk.gov.justice.digital.backend.converter

import jakarta.inject.Singleton

/**
 * Class encapsulating the logic that ensures a domain query will run against the target data source when running a
 * preview for a given domain.
 *
 * For now this targets the naming conventions in use in Athena.
 */
@Singleton
class DomainToPreviewQueryConverter {

    fun convertQuery(query: String, limit: Int): String {
        val cleanedQuery = query.replace("\n", "").trim()
        val queryTerms =
            cleanedQuery
                .split(" ")
                .filter { it.isNotEmpty() }

        return queryTerms
            .joinToString(" ") { convertTableNames(it) } + " limit $limit"
    }

    private val sourceNamePrefix = "^\\w+\\$InputSourceDelimiter\\w+.*$".toRegex()

    private fun convertTableNames(term: String): String {
        // We assume a naming convention of sourceName + InputSourceDelimiter [ + tableName + . + fieldName ]
        // For example both of the following table names will require modification
        //      nomis.offenders
        //      nomis.offenders.id
        // In both cases the first occurence of 'InputSourceDelimiter' will be replaced with OutputSourceDelimiter
        // yielding the following strings
        //      nomis_offenders
        //      nomis_offenders.id
        return if (sourceNamePrefix.matches(term)) term.replaceFirst(InputSourceDelimiter, OutputSourceDelimiter)
        else term
    }

    companion object {
        // TODO - this will be the default if no value is found in config
        const val InputSourceDelimiter = "."
        const val OutputSourceDelimiter = "_"
        val tableNameRegex = "^\\w+\\$InputSourceDelimiter\\w+.*$".toRegex()
    }
}