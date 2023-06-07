package uk.gov.justice.digital.command

import jakarta.inject.Singleton
import org.jline.keymap.BindingReader
import org.jline.keymap.KeyMap
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.terminal.Terminal.Signal
import org.jline.utils.Curses
import org.jline.utils.InfoCmp.Capability
import org.jline.utils.Status
import picocli.CommandLine.Command
import picocli.CommandLine.ParentCommand
import uk.gov.justice.digital.DomainBuilder
import uk.gov.justice.digital.service.DomainService
import uk.gov.justice.digital.session.ConsoleSession

// TODO - factor out the 'editor' functionality into a separate DomainEditor component that can be used by create/edit
//      - consider using ...digital.interactive.DomainEditor since this will only be available for interactive sessions.
@Singleton
@Command(
        name = "create",
        description = ["Create a new domain"],
)
class CreateDomain(private val service: DomainService) : Runnable {

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

sealed interface Element {
    val canSelect: Boolean
    fun render(): String
}
class Blank: Element {
    override val canSelect = false
    override fun render(): String { return "" }
}
data class InputField(val name: String,
                      val value: String,
                      val selected: Boolean = false,
                      val margin: Int = name.length): Element {
    override val canSelect = true
    override fun render(): String {
        val formattedName = String.format("%-${margin}s",  name)
        val formattedValue = String.format("%-30s", value)
        return if (selected) "@|bold $formattedName |@| @|bg(yellow),fg(black) $formattedValue |@"
            else "@|bold $formattedName |@| $formattedValue"

    }
}
data class Heading(val heading: String, val color: String): Element {
    override val canSelect = false
    override fun render(): String {
        return "@|$color,bold $heading|@"
    }
}

// This is modelled after the Less command which shows how we can take over the console and handle user key presses
// directly.
class DomainEditor(private val terminal: Terminal,
                   private val session: ConsoleSession) {

    private val bindingReader: BindingReader = BindingReader(terminal.reader())
    private val terminalSize: Size = Size()

    // Initial position when editing - defaults to first field
    private var position = 0

    private var pageElements = listOf(
            Blank(),
            Heading("Create domain", "green"),
            Blank(),
            InputField("Name", ""),
            InputField("Description", ""),
            InputField("Originator", ""),
            Blank(),
            Heading("Table Properties", "yellow"),
            Blank(),
            InputField("Table", ""),
            InputField("Description", ""),
            InputField("Sources", ""),
            InputField("Query", ""),
    )

    private val inputFieldMargin = pageElements
            .filter { it.canSelect && it is InputField }
            .maxOfOrNull { if (it is InputField) it.name.length else 0 }

    private val selectableElementIndexes = pageElements
            .withIndex()
            .filter { it.value.canSelect }
            .map { it.index }

    // Bounds for user cursor up/down
    private val minPosition = 0
    private val maxPosition = selectableElementIndexes.size - 1

    private fun updateDisplay() {
        val width = terminalSize.columns
        // TODO - handle case where there's too much output to display.
        val height = terminalSize.rows

        clearDisplay()

        // Update state - TODO - separate method
        val selectedElementIndex = selectableElementIndexes.get(position)
        pageElements = pageElements
                .withIndex()
                .map { item ->
                    val selected = item.index == selectedElementIndex
                    when(val element = item.value) {
                        is Blank -> element
                        is Heading -> element
                        // TODO - fix this - ideally the margin isn't nullable
                        is InputField -> element.copy(selected = selected, margin = inputFieldMargin ?: 20)
                    }
                }

        pageElements.forEach { e ->
            val content = e.render()
            if (content.isNotEmpty()) println(session.toAnsi(content))
            else println()
        }

        // TODO - cursor position should move too
        println()
    }

    private fun clearDisplay() {
        terminal.puts(Capability.clear_screen)
        terminal.flush()
    }

    private fun handleSignal(signal: Signal) {
        terminalSize.copy(terminal.size)
        updateDisplay()
    }

    // Allow ctrl-c to interrupt the command.
    private fun checkInterrupted() {
        Thread.yield()
        if (Thread.currentThread().isInterrupted) {
            throw InterruptedException()
        }
    }

    fun run() {
        val status = Status.getStatus(terminal);

        status?.suspend()

        terminalSize.copy(terminal.size)

        terminal.handle(Signal.WINCH, this::handleSignal)
        terminal.enterRawMode()

        val keys = KeyMap<Operation>()

        bindKeys(keys)

        terminal.writer().flush()

        updateDisplay()
        checkInterrupted()

        var continueRunning = true

        do {
            checkInterrupted()

            when (bindingReader.readBinding(keys, null, false)) {
                Operation.EXIT -> continueRunning = false
                Operation.UP -> updatePosition(-1)
                Operation.DOWN -> updatePosition(1)
                // For now the following operations are no-ops
                Operation.LEFT -> {}
                Operation.RIGHT -> {}
                null -> {}
            }

            updateDisplay()
        } while (continueRunning)
    }

    private fun updatePosition(i: Int) {
        val newPosition = position + i
        position = if (newPosition < minPosition) minPosition
            else if (newPosition > maxPosition) maxPosition
            else newPosition
    }

    private fun bindKeys(map: KeyMap<Operation>) {
        map.bind(Operation.EXIT, "q")
        map.bind(Operation.UP, "k", key(terminal, Capability.key_up))
        map.bind(Operation.DOWN, "j", key(terminal, Capability.key_down))
        map.bind(Operation.LEFT, key(terminal, Capability.key_left))
        map.bind(Operation.RIGHT, key(terminal, Capability.key_right))
    }

    private fun key(terminal: Terminal, capability: Capability): String {
        return Curses.tputs(terminal.getStringCapability(capability))
    }

    private enum class Operation {
        // General
        EXIT,
        // Motion
        UP,
        DOWN,
        LEFT,
        RIGHT,
    }

}