package uk.gov.justice.digital.backend.converter

import jakarta.inject.Singleton
import kotlin.math.min

/**
 * Class encapsulating the logic that ensures a domain query will run against the target data source when running a
 * preview for a given domain.
 *
 * For now this targets the naming conventions in use in Athena.
 */
@Singleton
class DomainToPreviewQueryConverter {

    fun convertQuery(query: String, limit: Int): String {
        val convertedQuery =
            query.replace("\n", "").trim()                   // Strip newlines
                .split(" ")                                  //  and split string on spaces
                .filter { it.isNotEmpty() }                  //  and remove empty strings
                .joinToString(" ") { convertTableNames(it) } //  and convert table names where found then combine into a single string
                .split("=")                                  // Now split on any equality tests which may not be space delimited
                .joinToString("=") { convertTableNames(it) } //  and convert table names where found and combine into a single string

        return convertedQuery.withLimitClause(limit)
    }

    private fun convertTableNames(term: String): String {
        // We assume a naming convention of sourceName + InputSourceDelimiter [ + tableName + . + fieldName ]
        // For example both of the following table names will require modification
        //      nomis.offenders
        //      nomis.offenders.id
        // In both cases the first occurence of 'InputSourceDelimiter' will be replaced with OutputSourceDelimiter
        // yielding the following strings
        //      nomis_offenders
        //      nomis_offenders.id
        return if (tableNameRegex.matches(term)) term.replaceFirst(InputSourceDelimiter, OutputSourceDelimiter)
        else term
    }

    private fun String.withLimitClause(limit: Int): String =
        limitClauseRegex.matchEntire(this)?.let {
            val (limitKeyword, limitValue) = it.destructured
            val validatedLimitValue = min(limitValue.toInt(), limit)
            val newLimitClause = "$limitKeyword$validatedLimitValue"
            this.replace("$limitKeyword$limitValue", newLimitClause)
        } ?: "$this limit $limit"

    companion object {
        // TODO - this will be the default if no value is found in config
        const val InputSourceDelimiter = "."
        const val OutputSourceDelimiter = "_"
        val tableNameRegex = "^\\w+\\$InputSourceDelimiter\\w+.*$".toRegex()
        val limitClauseRegex = "^.*(LIMIT\\s*)(\\d+).*$".toRegex(RegexOption.IGNORE_CASE)
    }
}