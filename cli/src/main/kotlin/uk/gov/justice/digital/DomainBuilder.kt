package uk.gov.justice.digital

import io.micronaut.configuration.picocli.MicronautFactory
import io.micronaut.configuration.picocli.PicocliRunner
import jakarta.inject.Singleton
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import uk.gov.justice.digital.command.CreateDomain
import uk.gov.justice.digital.command.ListDomains
import uk.gov.justice.digital.command.ViewDomain
import uk.gov.justice.digital.session.BatchSession
import uk.gov.justice.digital.session.InteractiveSession
import java.lang.IllegalStateException

@Command(
    name = "domain-builder",
    subcommands = [
        ListDomains::class,
        ViewDomain::class,
        CreateDomain::class,
    ],
)
@Singleton
class DomainBuilder(
    private val batchSession: BatchSession,
    private val interactiveSession: InteractiveSession
) : Runnable {

    @Option(
        names = ["-h", "--help"],
        usageHelp = true,
        description = [ "display this help message" ]
    )
    var usageHelpRequested = false

    @Option(
        names = ["-i", "--interactive"],
        description = ["Run domain-builder in interactive mode"],
        required = false
    )
    private var isInteractive = false

    @Option(
        names = ["--enable-ansi"],
        description = ["Enable ANSI formatting in interactive sessions"],
        required = false
    )
    private var ansiEnabled = false

    override fun run() {
        System.setProperty("picocli.ansi", "$ansiEnabled")

        if (isInteractive) {
            val commandLine = CommandLine(this, MicronautFactory())
            interactiveSession.start(commandLine)
        }
    }

    fun print(s: String) {
        if (isInteractive) interactiveSession.print(s)
        else batchSession.print(s)
    }

    fun getInteractiveSession(): InteractiveSession {
        if (isInteractive) return interactiveSession
        else throw IllegalStateException("Cannot get interactive session. Current session is not interactive")
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            // TODO - is there a better way to launch help?
            if (args.isEmpty()) {
                val fakeArgs = arrayOf("--help")
                PicocliRunner.execute(DomainBuilder::class.java, *fakeArgs)
            } else PicocliRunner.execute(DomainBuilder::class.java, *args)
        }

    }
}
