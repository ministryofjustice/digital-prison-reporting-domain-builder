package uk.gov.justice.digital.backend.converter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DomainToPreviewQueryConverterTest {

    private val underTest = DomainToPreviewQueryConverter()

    @Test
    fun `it should convert a simple query referring to a single table`() {
        val query = """
            SELECT nomis.agency_locations.agy_loc_id as id, nomis.agency_locations.description as name 
            from nomis.agency_locations
        """.trimIndent()

        val result = underTest.convertQuery(query, 10)

        assertEquals(
            "SELECT nomis_agency_locations.agy_loc_id as id, nomis_agency_locations.description as name from nomis_agency_locations limit 10",
            result
        )
    }

    @Test
    fun `it should convert a query with a join referring to two tables`() {

    }

    @Test
    fun `it should modify an existing limit clause where that limit exceeds that required for the preview`() {

    }

}