package uk.gov.justice.digital.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.service.StaticData.domain1
import uk.gov.justice.digital.service.StaticData.domain2
import uk.gov.justice.digital.service.StaticData.domain3
import java.util.*

class DomainServiceTest {

    private val underTest = DomainService()

    @Test
    fun `getDomains should return all domains when no filters are specified`() {
        assertEquals(listOf(domain1, domain2, domain3), underTest.getDomains())
    }

    @Test
    fun `getDomains should return a single domain when a name filter is specified and a matching domain exists`() {
        assertEquals(listOf(domain3), underTest.getDomains(name = "Domain 3"))
    }

    @Test
    fun `getDomains should return an empty list where no matching domain exists with the given name`() {
        assertEquals(emptyList<Domain>(), underTest.getDomains(name = "no domain with this name exists"))
    }

    @Test
    fun `getDomain should return a domain for the given UUID where a matching domain exists`() {
        assertEquals(domain2, underTest.getDomain(domain2.id))
    }

    @Test
    fun `getDomain should return null for the given UUID where no matching domain exists`() {
        assertNull(underTest.getDomain(UUID.randomUUID()))
    }

}