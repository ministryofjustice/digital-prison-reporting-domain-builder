package uk.gov.justice.digital

import io.micronaut.configuration.picocli.MicronautFactory
import io.micronaut.configuration.picocli.PicocliRunner
import jakarta.inject.Singleton
import org.fusesource.jansi.AnsiConsole
import org.jline.builtins.ConfigurationPath
import org.jline.console.CmdLine
import org.jline.console.SystemRegistry
import org.jline.console.impl.Builtins
import org.jline.console.impl.SystemRegistryImpl
import org.jline.keymap.KeyMap
import org.jline.reader.*
import org.jline.reader.impl.DefaultParser
import org.jline.terminal.TerminalBuilder
import org.jline.widget.TailTipWidgets
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.HelpCommand
import picocli.CommandLine.Option
import picocli.shell.jline3.PicocliCommands
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
class DomainBuilder : Runnable {

    @Option(
        names = ["-i", "--interactive"],
        description = ["Run the command in interactive mode"],
        required = false
    )
    var interactive = false

    private var out: PrintWriter? = null;

    fun setReader(reader: LineReader) {
        out = reader.terminal.writer()
    }

    override fun run() {
        if (interactive) {
            println("Launching interactive shell")

            // The following is a straight copy and paste from the docs - tidy up as required
            AnsiConsole.systemInstall()
            try {
                val workDir: Supplier<Path> = Supplier {
                    Paths.get(
                        System.getProperty(
                            "user.dir"
                        )
                    )
                }
                // set up JLine built-in commands
                // TODO - does it matter what we set ConfigurationPath to? For now we just set it to /tmp
                val tmpPath = Paths.get("/tmp")
                val builtins = Builtins(workDir, ConfigurationPath(tmpPath, tmpPath), null)
                builtins.rename(Builtins.Command.TTOP, "top")
                builtins.alias("zle", "widget")
                builtins.alias("bindkey", "keymap")
                // set up picocli commands
                val commands = this
                val factory = MicronautFactory()
                val cmd = CommandLine(commands, factory)
                val picocliCommands = PicocliCommands(cmd)
                val parser: Parser = DefaultParser()

                TerminalBuilder.builder().build().use { terminal ->
                    val systemRegistry: SystemRegistry = SystemRegistryImpl(parser, terminal, workDir, null)
                    systemRegistry.setCommandRegistries(builtins, picocliCommands)
                    systemRegistry.register("help", picocliCommands)
                    val reader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .completer(systemRegistry.completer())
                        .parser(parser)
                        .variable(LineReader.LIST_MAX, 50) // max tab completion candidates
                        .build()
                    builtins.setLineReader(reader)
                    commands.setReader(reader)
                    // TODO - what other widgets are there?
                    val widgets = TailTipWidgets(
                        reader,
                        { line: CmdLine? -> systemRegistry.commandDescription(line) },
                        5,
                        TailTipWidgets.TipType.COMPLETER
                    )
                    widgets.enable()
                    val keyMap: KeyMap<Binding>? = reader.getKeyMaps().get("main")
                    keyMap?.bind(Reference("tailtip-toggle"), KeyMap.alt("s"))
                    val prompt = "prompt> "
                    val rightPrompt: String? = null

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
            PicocliRunner.execute(DomainBuilder::class.java, *args);
        }
    }

}
