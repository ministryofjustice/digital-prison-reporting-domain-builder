package uk.gov.justice.digital.cli.command

import jakarta.inject.Singleton
import picocli.CommandLine.*
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.command.ExceptionHandler.runAndHandleExceptions
import uk.gov.justice.digital.cli.service.DomainService
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.text.Charsets.UTF_8

@Singleton
@Command(
    name = "create-from-json",
    description = ["Create a domain from a JSON definition"]
)
class CreateDomain(private val service: DomainService) : Runnable {

    @Option(
        names = ["-h", "--help"],
        description = [ "display this help message" ]
    )
    var usageHelpRequested = false

    @Option(
        names = ["-f", "--file"],
        description = [ "file that contains a JSON domain definition" ],
        arity = "1",
        required = true,
        paramLabel = "FILE_NAME",
    )
    lateinit var filename: String

    @ParentCommand
    lateinit var parent: DomainBuilder

    override fun run() =
        runAndHandleExceptions(parent) {
            if (Path(filename).exists()) processJson(File(filename).readText(UTF_8))
            else parent.print("@|red,bold File $filename not found|@")
        }

    private fun processJson(json: String) {
        val result = service.createDomain(json)
        parent.print("@|bold,green Domain successfully created with id: $result|@")
    }

}