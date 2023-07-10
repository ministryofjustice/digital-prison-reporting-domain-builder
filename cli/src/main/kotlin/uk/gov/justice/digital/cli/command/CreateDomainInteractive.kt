package uk.gov.justice.digital.cli.command

import jakarta.inject.Singleton
import picocli.CommandLine.Command
import picocli.CommandLine.ParentCommand
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.editor.DomainEditor
import uk.gov.justice.digital.cli.service.DomainService

// TODO - this should only come into play when we are in an interactive session
@Singleton
@Command(
        name = "create-interactive",
        description = ["Create a new domain"],
)
class CreateDomainInteractive(private val service: DomainService) : Runnable {

    @ParentCommand
    lateinit var parent: DomainBuilder

    private val editor: DomainEditor by lazy {
        DomainEditor(parent.getInteractiveSession(), service)
    }

    override fun run() {
        editor.run()
    }

}

