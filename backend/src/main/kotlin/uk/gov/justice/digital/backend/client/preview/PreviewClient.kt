package uk.gov.justice.digital.backend.client.preview

interface PreviewClient {

    fun runQuery(query: String): List<List<String>>

}