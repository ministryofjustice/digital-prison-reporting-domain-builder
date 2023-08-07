package uk.gov.justice.digital.backend.service

import uk.gov.justice.digital.model.Status

interface PreviewService {
    fun preview(domainName: String, status: Status, limit: Int): List<List<String>>

    companion object {
        const val MaximumLimit = 100
    }
}