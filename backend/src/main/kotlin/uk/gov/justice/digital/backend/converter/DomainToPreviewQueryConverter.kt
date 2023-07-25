package uk.gov.justice.digital.backend.converter

import io.micronaut.context.annotation.Value
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

    @Value("\${preview.inputSourceDelimiter")
    private val inputSourceDelimiter = "."

    @Value("\${preview.outputSourceDelimiter")
    private val outputSourceDelimiter = "_"

    private val tableNameRegex = "^\\w+\\$inputSourceDelimiter\\w+.*$".toRegex()
    private val limitClauseRegex = "^.*(LIMIT\\s*)(\\d+).*$".toRegex(RegexOption.IGNORE_CASE)

    fun convertQuery(query: String, limit: Int): String {
        val convertedQuery =
            query.replace("\n", "")
                .trim()
                .split(" ")
                .filter { it.isNotEmpty() }
                .joinToString(" ") { convertTableNames(it) }
                .split("=")
                .joinToString("=") { convertTableNames(it) }

        return convertedQuery.withLimitClause(limit)
    }

    private fun convertTableNames(term: String): String {
        // We assume a naming convention of sourceName + InputSourceDelimiter [ + tableName + . + fieldName ]
        // For example both of the following table names will require modification
        //      nomis.offenders
        //      nomis.offenders.id
        // In both cases the first occurrence of 'InputSourceDelimiter' will be replaced with OutputSourceDelimiter
        // yielding the following strings
        //      nomis_offenders
        //      nomis_offenders.id
        return if (tableNameRegex.matches(term)) term.replaceFirst(inputSourceDelimiter, outputSourceDelimiter)
        else term
    }

    private fun String.withLimitClause(limit: Int): String =
        limitClauseRegex.matchEntire(this)?.let {
            val (limitKeyword, limitValue) = it.destructured
            val validatedLimitValue = min(limitValue.toInt(), limit)
            val newLimitClause = "$limitKeyword$validatedLimitValue"
            this.replace("$limitKeyword$limitValue", newLimitClause)
        } ?: "$this limit $limit"

}