package uk.gov.justice.digital.backend.validator

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class SparkSqlValidator {

    private val logger = LoggerFactory.getLogger(this::class.java)

    // TODO - reimplement Spark SQL validation. See DPR-600.
    fun validate(sparkSql: String): SparkSqlValidationResult {
        logger.warn("Spark SQL validation not implemented. Treating query '{}' as valid.", sparkSql)
        return ValidSparkSqlResult()
    }

}

sealed interface SparkSqlValidationResult { val isValid: Boolean }
class ValidSparkSqlResult : SparkSqlValidationResult { override val isValid: Boolean = true }
class InvalidSparkSqlResult(val reason: String) : SparkSqlValidationResult { override val isValid: Boolean = false }