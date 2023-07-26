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
        val query = """
            select nomis.offender_external_movements.movement_seq as id, nomis.offender_external_movements.offender_book_id as prisoner, nomis.offender_e
            xternal_movements.movement_date as date,  nomis.offender_external_movements.movement_time as time, nomis.offender_external_movements.from_agy
            _loc_id as origin, nomis.offender_external_movements.to_agy_loc_id as destination, omw_owner.movement_reasons.description as reason FROM nomis.of
            fender_external_movements JOIN omw_owner.movement_reasons ON omw_owner.movement_reasons.movement_type=nomis.offender_external_movements.movement_type
             and omw_owner.movement_reasons.movement_reason_code=nomis.offender_external_movements.movement_reason_code
        """.trimIndent()

        val result = underTest.convertQuery(query, 10)

        assertEquals(
            "select nomis_offender_external_movements.movement_seq as id, nomis_offender_external_movements.offender_book_id as prisoner, nomis_offender_external_movements.movement_date as date, nomis_offender_external_movements.movement_time as time, nomis_offender_external_movements.from_agy_loc_id as origin, nomis_offender_external_movements.to_agy_loc_id as destination, omw_owner_movement_reasons.description as reason FROM nomis_offender_external_movements JOIN omw_owner_movement_reasons ON omw_owner_movement_reasons.movement_type=nomis_offender_external_movements.movement_type and omw_owner_movement_reasons.movement_reason_code=nomis_offender_external_movements.movement_reason_code limit 10",
            result
        )
    }

    @Test
    fun `it should modify an existing limit clause where it exceeds that requested for the preview`() {
        val query = """
            select * from some_table limit 5000
        """.trimIndent()

        val result = underTest.convertQuery(query, 10)

        assertEquals(
            "select * from some_table limit 10",
            result
        )
    }

    @Test
    fun `it should retain an existing limit clause where it is smaller than that requested for the preview`() {
        val query = """
            select * from some_table limit 5
        """.trimIndent()

        val result = underTest.convertQuery(query, 10)

        assertEquals(
            "select * from some_table limit 5",
            result
        )
    }


}