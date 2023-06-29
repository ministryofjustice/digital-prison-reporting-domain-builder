package uk.gov.justice.digital.backend.validator

import jakarta.inject.Singleton
import org.apache.spark.sql.execution.SparkSqlParser
import org.apache.spark.sql.internal.SQLConf
import org.slf4j.LoggerFactory

@Singleton
class SparkSqlValidator {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val validator = SparkSqlParser(SQLConf())

    fun validate(sparkSql: String): SparkSqlValidationResult {
        return try {
            validator.parseExpression(sparkSql)
            ValidSparkSql()
        } catch (ex: Exception) {
            logger.warn("Failed to validate spark SQL: '{}'", sparkSql)
            InvalidSparkSql(ex.localizedMessage)
        }
    }

}

sealed interface SparkSqlValidationResult { val isValid: Boolean }
class ValidSparkSql : SparkSqlValidationResult { override val isValid: Boolean = true }
class InvalidSparkSql(val reason: String) : SparkSqlValidationResult { override val isValid: Boolean = false }