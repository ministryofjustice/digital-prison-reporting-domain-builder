package uk.gov.justice.digital.command

import jakarta.inject.Singleton
import picocli.CommandLine.Command
import uk.gov.justice.digital.service.DomainService

@Singleton
@Command(
    name = "view",
    description = ["View details for a specific domain"]
)
class ViewDomain(private val service: DomainService) : Runnable {

    override fun run() {
        TODO("Not yet implemented")
    }

}