package uk.gov.justice.digital.cli.command

import jakarta.inject.Singleton
import org.jline.keymap.BindingReader
import org.jline.keymap.KeyMap
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.terminal.Terminal.Signal
import org.jline.utils.Display
import org.jline.utils.InfoCmp.Capability
import picocli.CommandLine.Command
import picocli.CommandLine.ParentCommand
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.service.DomainService
import uk.gov.justice.digital.cli.session.ConsoleSession
import java.lang.Integer.min
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

sealed interface Element {
    val canSelect: Boolean
    fun render(width: Int): String
}

class Blank: Element {
    override val canSelect = false
    override fun render(width: Int): String { return " ".repeat(width) }
}

data class Field(val name: String,
                 val value: String,
                 val selected: Boolean = false,
                 val margin: Int = name.length): Element {

    override val canSelect = true
    override fun render(width: Int): String {
        val formattedName = String.format("%-${margin}s",  name)
        val padding = " ".repeat(width - margin - value.length - 6)
        return if (selected) "@|bold  $formattedName |@│ @|underline $value$padding |@ "
            else "@|bold,faint  $formattedName |@│ $value$padding  "

    }
}

data class Heading(val heading: String, val color: String, val backgroundColor: String? = null): Element {
    override val canSelect = false
    override fun render(width: Int): String {
        val bgString = backgroundColor?.let { ",bg($it)" } ?: ""
        val padding = " ".repeat(width - heading.length - 1)
        return "@|fg($color)$bgString,bold  $heading$padding|@"
    }
}

// This is modelled after the Less command which shows how we can take over the console and handle user key presses
// directly.
class DomainEditor(private val terminal: Terminal,
                   private val session: ConsoleSession) {

    private val bindingReader: BindingReader = BindingReader(terminal.reader())
    private val terminalSize: Size = Size()
    private val display = Display(terminal, true)
    private val originalAttributes = terminal.attributes

    // Initial position when editing - defaults to first editable field
    private var selectedField = 0

    private var pageElements = emptyPageElements()

    private val inputFieldMargin = pageElements
        .filter { it.canSelect && it is Field }
        .map { it as Field }
        .maxOf { it.name.length }

    private val selectableElementIndexes = pageElements
            .withIndex()
            .filter { it.value.canSelect }
            .map { it.index }

    // Bounds for user cursor up/down
    private val minPosition = 0
    private val maxPosition = selectableElementIndexes.size - 1

    // TODO - tags, allow key value pair entry / deletion
    // TODO - the following fits into an 80 x 24 window - if more entries are required we will need to handle this
    //        by defining a window or offset to only show those values that fit as the user moves up and down.
    private fun emptyPageElements() = listOf(
        Blank(),
        Heading("Create New Domain", "black", "green"),
        Blank(),
        Heading("Domain Properties", "yellow"),
        Blank(),
        Field("Name", ""),
        Field("Description", ""),
        Field("Location", ""),
        Field("Owner", ""),
        Field("Author", ""),
        Blank(),
        Heading("Table Properties", "yellow"),
        Blank(),
        Field("Name", ""),
        Field("Description", ""),
        Field("Location", ""),
        Field("Owner", ""),
        Field("Author", ""),
        Field("Primary Key", ""),
        Field("Sources", ""),
        Field("Spark Query", ""),
        Blank(),
        Heading("keys │ ↑ move up │ ↓ move down │ enter to edit │ s to Save │ q to Quit ", "black", "white")
    )

    // TODO - try to integrate this if possible?
    private fun updateSelectedElement(input: String?) {
        val selectedElementIndex = selectableElementIndexes[selectedField]
        pageElements = pageElements
            .withIndex()
            .map { item ->
                val selected = item.index == selectedElementIndex
                when(val element = item.value) {
                    is Blank -> element
                    is Heading -> element
                    // TODO - fix this - ideally the margin isn't nullable
                    // TODO - cursor position should be updated too
                    is Field -> element.copy(
                        selected = selected,
                        margin = inputFieldMargin ?: 20, // TODO - define default elsewhere
                        value = if (selected && input != null) input else element.value
                    )
                }
            }
    }

    private fun updateDisplay(input: String? = null) {
        val width = terminalSize.columns

        // By default hide the cursor until the user enters edit mode
        hideCursor()

        moveCursorToHome()

        val selectedElementIndex = selectableElementIndexes[selectedField]

        pageElements = pageElements
                .withIndex()
                .map { item ->
                    val selected = item.index == selectedElementIndex
                    when(val element = item.value) {
                        is Blank -> element
                        is Heading -> element
                        is Field -> element.copy(
                                selected = selected,
                                margin = inputFieldMargin,
                                value = if (selected && input != null) input else element.value
                        )
                    }
                }

        pageElements.forEach { e ->
            val content = e.render(width)
            if (content.isNotEmpty()) println(session.toAnsi(content))
            else println()
        }

        // Reset cursor position and move to the currently selected element
        terminal.puts(Capability.cursor_home)

        val selectedElement = pageElements[selectedElementIndex]
        val inputLength = if (selectedElement is Field) selectedElement.value.length else 0

        moveCursorTo(selectedElementIndex, inputLength + 15)

        terminal.flush()
    }

    private fun moveCursorToHome() {
        terminal.puts(Capability.cursor_home)
        terminal.flush()
    }

    private fun moveCursorTo(line: Int, column: Int) {
        moveCursorToHome()
        // TODO - try escape codes with parameters again. Should be possible to include the number of moves in a
        //        single code e.g. ESC[12A but it's not worked so far hence the loops below.
        moveCursorDown(line)
        moveCursorRight(column)
    }

    private fun moveCursorDown(lines: Int) = applyAction(Capability.cursor_down, lines)
    private fun moveCursorRight(columns: Int) = applyAction(Capability.cursor_right, columns)
    private fun moveCursorLeft(columns: Int) = applyAction(Capability.cursor_left, columns)

    private fun applyAction(action: Capability, count: Int = 1) {
        for (i in 1..count) { terminal.puts(action) }
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

    private fun resetState() {
        pageElements = emptyPageElements()
        selectedField = 0
    }

    private fun enableRawMode() {
        terminal.enterRawMode()
    }

    private fun disableRawMode() {
        terminal.attributes = originalAttributes
    }

    fun run() {
        resetState()
        clearDisplay()

        // TODO - can we clean this up using an immutable map instead?
        val keys = KeyMap<Operation>()
        bindKeys(keys)

        terminalSize.copy(terminal.size)
        terminal.handle(Signal.WINCH, this::handleSignal)
        display.reset()

        enableRawMode()

        updateDisplay()

        var continueRunning = true

        do {
            checkInterrupted()

            when (bindingReader.readBinding(keys, null, true)) {
                Operation.EXIT -> {
                    // TODO - are you sure prompt
                    continueRunning = false
                    clearDisplay()
                    showCursor()
                    disableRawMode()
                }
                Operation.UP -> updatePositionAndRefreshDisplay(-1)
                Operation.DOWN -> updatePositionAndRefreshDisplay(1)
                // TODO - can we add ESC to abort editing?
                Operation.EDIT -> {

                    // TODO - provide a method to get the current input field or null?
                    val selectedElementIndex = selectableElementIndexes[selectedField]
                    val selectedElement = pageElements[selectedElementIndex] as? Field

                    if (selectedElement?.name == "Spark Query") {
                        showCursor()
                        terminal.flush()

                        clearDisplay()
                        println("Edit Spark Query")
                        println()

                        val editReader = BindingReader(terminal.reader())

                        val map = KeyMap<String>()
                        map.bind("up", "\u001B[A", "k")
                        map.bind("down", "\u001B[B", "j")
                        map.bind("right", "\u001B[C", "j")
                        map.bind("left", "\u001B[D", "j")
                        map.bind("enter", "\r")
                        map.bind("delete", "\b")

                        for (i in 32..255) {
                            // Only bind if the char is not DEL (ASCII 127)
                            if (i != 127) map.bind("insert", Character.toString(i))
                            else map.bind("delete", Character.toString(i))
                        }

                        val minLine = 0
                        val maxLine = 20
                        val minColumn = 0
                        val maxColumn = 79

                        var currentLine = 0
                        var currentColumn = 0

                        val lines = Array(20) { _ -> ""}

                        while(true) {
                            val result = editReader.readBinding(map, null, true)
                            when (result) {
                                "up" -> if (currentLine > minLine) {
                                    terminal.puts(Capability.cursor_up)
                                    currentLine--
                                }

                                "down" -> if (currentLine <= maxLine) {
                                    terminal.puts(Capability.cursor_down)
                                    currentLine++
                                }

                                "left" -> if (currentColumn > minColumn) {
                                    terminal.puts(Capability.cursor_left)
                                    currentColumn--
                                }
                                "right" -> if (currentColumn < maxColumn) {
                                    terminal.puts(Capability.cursor_right)
                                    currentColumn++
                                }
                                "insert" -> {
                                    if (currentColumn < maxColumn) {
                                        print(editReader.lastBinding)
                                        lines[currentLine] = lines[currentLine] + editReader.lastBinding
                                        currentColumn++
                                    }
                                    else if (currentLine < maxLine) {
                                        terminal.puts(Capability.cursor_down)
                                        print("\r")
                                        currentColumn = 0
                                        currentLine++
                                    }
                                }
                                "enter" -> if (currentLine < maxLine) {
                                    terminal.puts(Capability.cursor_down)
                                    print("\r")
                                    currentColumn = 0
                                    currentLine++
                                }
                                "delete" -> {
                                    val line = lines[currentLine]
                                    if (line.isNotEmpty()) {
                                        lines[currentLine] = line.removeRange(currentColumn - 1, currentColumn)
                                        terminal.puts(Capability.cursor_left)
                                        terminal.puts(Capability.delete_character)
                                    }
                                    else if (currentLine > minLine) {
                                        currentLine--
                                        terminal.puts(Capability.cursor_up)
                                        for(i in 0 until lines[currentLine].length) {
                                            terminal.puts(Capability.cursor_right)
                                            currentColumn++
                                        }
                                    }
                                }
                            }
                            terminal.flush()
////                            println("got $result")
////                            print(editReader.readStringUntil("\n"))
////                            println("buffer: ${editReader.currentBuffer}")
                        }

//                        var lineReader: LineReader? = null
//
//                        lineReader = LineReaderBuilder.builder()
//                            .terminal(this.terminal)
//                            .parser(MultilineParser(terminal))
//                            .build()
//
//                        val currentValue = selectedElement.value
//
//                        val input = lineReader.readLine(" ", null, currentValue)
//
//                        updateDisplay(input)
                    }
                    else {
                        disableRawMode()

                        val inputLength = selectedElement?.value?.length ?: 0

                        // Move the cursor to the start of the value string before we accept input since it will be in the
                        // wrong place if we already have a value (we default to placing the cursor at the end of the string).
                        moveCursorLeft(inputLength)

                        // TODO - can we support multiline input?
                        val lineReader = LineReaderBuilder.builder()
                            .terminal(this.terminal)
                            .build()

                        // TODO - this code is a bit messy - provide helper methods
                        val currentValue = selectedElement?.value ?: ""

                        // Since we need to set the prompt to a single space we need to shift the cursor left on char before
                        // we accept user input otherwise the cursor will not be properly aligned.
                        terminal.puts(Capability.cursor_left)
                        // Now make the cursor visible
                        showCursor()

                        val input = lineReader.readLine(" ", null, currentValue)

                        // Hide the cursor again as soon as the user has hit enter.
                        hideCursor()

                        enableRawMode()
                        updateDisplay(input)
                    }
                }
                else -> {
                    /* Do nothing on unhandled input */
                }
            }


        } while (continueRunning)
    }

    private fun showCursor() = terminal.puts(Capability.cursor_visible)

    private fun hideCursor() = terminal.puts(Capability.cursor_invisible)

    private fun updatePositionAndRefreshDisplay(i: Int) {
        // Ensure the updated value remains within the bounds minPosition < position < maxPosition
        // TODO - check kotlin syntax - does it provide anything to express this?
        val newPosition = min(maxPosition, max(minPosition, selectedField + i))
        // Only update the display if the position has changed
        if (newPosition != selectedField) {
            selectedField = newPosition
            updateDisplay()
        }
    }

    private fun bindKeys(map: KeyMap<Operation>) {
        map.bind(Operation.EXIT, "q")
        map.bind(Operation.UP, "\u001B[A", "k")
        map.bind(Operation.DOWN, "\u001B[B", "j")
        map.bind(Operation.EDIT, "\r")
    }

    // TODO - we need a SAVE operation
    private enum class Operation {
        // General
        EXIT,
        // Motion
        UP,
        DOWN,
        // Editing
        EDIT
    }


}