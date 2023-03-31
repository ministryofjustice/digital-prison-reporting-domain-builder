package uk.gov.justice.digital.service

/**
 * Initial service that will handle calls to the backend API and processing the results.
 *
 * For now this just provides hardcoded data to support the development of the CLI.
 *
 * See DPR-363 which covers the work to integrate the CLI with the backend API.
 */
class DomainService {

    fun getAllDomains(): Array<String> {
        return emptyArray<String>()
    }

}