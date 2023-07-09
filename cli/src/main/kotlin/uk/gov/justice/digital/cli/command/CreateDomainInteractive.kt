package uk.gov.justice.digital.cli.command

import jakarta.inject.Singleton
import picocli.CommandLine.Command
import picocli.CommandLine.ParentCommand
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.service.DomainService
import kotlin.math.max

@Singleton
@Command(
        name = "create-interactive",
        description = ["Create a new domain"],
)
class CreateDomainInteractive(private val service: DomainService) : Runnable {

    @ParentCommand
    lateinit var parent: DomainBuilder

    // TODO - revise to pass in the session only
    private val editor: DomainEditor by lazy {
        DomainEditor(parent.getInteractiveSession().terminal(),
                     parent.getInteractiveSession())
    }

    override fun run() {
        editor.run()
    }

}

