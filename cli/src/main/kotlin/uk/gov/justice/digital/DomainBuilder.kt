package uk.gov.justice.digital

import io.micronaut.configuration.picocli.MicronautFactory
import io.micronaut.configuration.picocli.PicocliRunner
import jakarta.inject.Singleton
import org.fusesource.jansi.AnsiConsole
import org.jline.console.SystemRegistry
import org.jline.console.impl.SystemRegistryImpl
import org.jline.reader.*
import org.jline.reader.impl.DefaultParser
import org.jline.terminal.TerminalBuilder
import org.jline.widget.AutosuggestionWidgets
import picocli.CommandLine
import picocli.CommandLine.*
import picocli.shell.jline3.PicocliCommands
import uk.gov.justice.digital.command.CommandBase
import uk.gov.justice.digital.command.ListDomains
import uk.gov.justice.digital.command.ViewDomain
import java.io.PrintWriter
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Supplier

@Command(
    name = "domain-builder",
    mixinStandardHelpOptions = true,
    version = ["domain-builder 0.0.1"],
    subcommands = [
        HelpCommand::class,
        ListDomains::class,
        ViewDomain::class,
    ],
)
@Singleton
class DomainBuilder : CommandBase(), Runnable {

    @Option(
        names = ["-i", "--interactive"],
        description = ["Run the command in interactive mode"],
        required = false
    )
    var interactive = false

    private var out: PrintWriter? = null

    private fun setReader(reader: LineReader) {
        out = reader.terminal.writer()
    }

    private val launchText = """
        @|bold,cyan Domain Builder|@
        
        Type @|bold help|@ to view available commands.
        
        Type @|bold <command> --help|@ to view help for a specific command.
        
        Type @|bold exit|@ to exit the domain builder.
        
        Press the @|bold TAB|@ key to view available commands or autocomplete commands as you type.
        
    """.trimIndent()

    override fun run() {
        if (interactive) {
            printlnAnsi(launchText)

            AnsiConsole.systemInstall()

            try {
                val workDir: Supplier<Path> = Supplier {
                    Paths.get(
                        System.getProperty(
                            "user.dir"
                        )
                    )
                }

                // Set up picocli commands
                val commands = this
                val factory = MicronautFactory()
                val cmd = CommandLine(commands, factory)
                val picocliCommands = PicocliCommands(cmd)
                val parser: Parser = DefaultParser()

                TerminalBuilder.builder().build().use { terminal ->
                    val systemRegistry: SystemRegistry = SystemRegistryImpl(parser, terminal, workDir, null)
                    systemRegistry.setCommandRegistries(picocliCommands)
                    systemRegistry.register("help", picocliCommands)

                    val reader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .completer(systemRegistry.completer())
                        .parser(parser)
                        .variable(LineReader.LIST_MAX, 50) // max tab completion candidates
                        .build()
                    commands.setReader(reader)

                    val autosuggestionWidgets = AutosuggestionWidgets(reader)
                    autosuggestionWidgets.enable()

                    val prompt = "domain-builder> "
                    val rightPrompt = ""

                    // start the shell and process input until the user quits with Ctrl-D
                    while (true) {
                        try {
                            systemRegistry.cleanUp()
                            val line = reader.readLine(prompt, rightPrompt, null as MaskingCallback?, null)
                            systemRegistry.execute(line)
                        } catch (e: UserInterruptException) {
                            // Ignore
                        } catch (e: EndOfFileException) {
                            return
                        } catch (e: Exception) {
                            systemRegistry.trace(e)
                        }
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                AnsiConsole.systemUninstall()
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("picocli.ansi", "true")
            PicocliRunner.execute(DomainBuilder::class.java, *args)
        }
    }

}
