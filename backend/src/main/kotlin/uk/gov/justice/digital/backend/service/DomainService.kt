package uk.gov.justice.digital.backend.service

import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.model.WriteableDomain
import java.util.*

interface DomainService {
    fun getDomains(name: String? = null, status: Status? = null): List<Domain>
    fun getDomain(id: UUID): Domain?
    fun createDomain(domain: WriteableDomain): UUID
    fun publishDomain(name: String, status: Status): UUID
}
