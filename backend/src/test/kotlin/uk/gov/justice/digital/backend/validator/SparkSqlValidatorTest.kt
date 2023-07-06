package uk.gov.justice.digital.backend.validator

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SparkSqlValidatorTest {

    private val underTest = SparkSqlValidator()

    @Test
    fun `should return a successful result given a valid SQL string`() {
        val validationResult = underTest.validate("select * from foo")
        assertTrue(validationResult.isValid)
    }

    @Disabled
    fun `should return an unsuccessful result given an invalid SQL string`() {
        val validationResult = underTest.validate("select a thing from foo")
        assertFalse(validationResult.isValid)
    }

}