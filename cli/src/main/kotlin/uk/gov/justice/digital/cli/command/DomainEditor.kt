package uk.gov.justice.digital.cli.command

import org.jline.keymap.BindingReader
import org.jline.keymap.KeyMap
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.utils.Display
import org.jline.utils.InfoCmp
import uk.gov.justice.digital.cli.session.ConsoleSession
import kotlin.math.max

// TODO - extract this
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
        MultiLineField("Spark Query", ""),
        Blank(),
        Heading("keys │ ↑ move up │ ↓ move down │ enter to edit │ s to Save │ q to Quit ", "black", "white")
    )

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
                    is MultiLineField -> element.copy(
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
        terminal.puts(InfoCmp.Capability.cursor_home)

        val selectedElement = pageElements[selectedElementIndex]
        val inputLength = if (selectedElement is Field) selectedElement.value.length else 0

        moveCursorTo(selectedElementIndex, inputLength + 15)

        terminal.flush()
    }

    private fun moveCursorToHome() {
        terminal.puts(InfoCmp.Capability.cursor_home)
        terminal.flush()
    }

    private fun moveCursorTo(line: Int, column: Int) {
        moveCursorToHome()
        // TODO - try escape codes with parameters again. Should be possible to include the number of moves in a
        //        single code e.g. ESC[12A but it's not worked so far hence the loops below.
        moveCursorDown(line)
        moveCursorRight(column)
    }

    private fun moveCursorDown(lines: Int) = applyAction(InfoCmp.Capability.cursor_down, lines)
    private fun moveCursorRight(columns: Int) = applyAction(InfoCmp.Capability.cursor_right, columns)
    private fun moveCursorLeft(columns: Int) = applyAction(InfoCmp.Capability.cursor_left, columns)

    private fun applyAction(action: InfoCmp.Capability, count: Int = 1) {
        for (i in 1..count) { terminal.puts(action) }
    }

    private fun clearDisplay() {
        terminal.puts(InfoCmp.Capability.clear_screen)
        terminal.flush()
    }

    private fun handleSignal(signal: Terminal.Signal) {
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

        val keys = bindKeys()

        terminalSize.copy(terminal.size)
        terminal.handle(Terminal.Signal.WINCH, this::handleSignal)
        display.reset()

        enableRawMode()

        updateDisplay()

        while(true) {
            checkInterrupted()

            when (bindingReader.readBinding(keys, null, true)) {
                Operation.EXIT -> {
                    handleExit()
                    break
                }
                Operation.UP -> updatePositionAndRefreshDisplay(-1)
                Operation.DOWN -> updatePositionAndRefreshDisplay(1)
                Operation.EDIT -> {
                    val selectedElementIndex = selectableElementIndexes[selectedField]
                    val selectedElement = pageElements[selectedElementIndex]

                    if (selectedElement is MultiLineField) handleMultiLineFieldEdit(selectedElement)
                    else if (selectedElement is Field) handleFieldEdit(selectedElement)
                }
                else -> {
                    /* Do nothing on unhandled input */
                }
            }


        }
    }

    private fun showCursor() = terminal.puts(InfoCmp.Capability.cursor_visible)

    private fun hideCursor() = terminal.puts(InfoCmp.Capability.cursor_invisible)

    private fun updatePositionAndRefreshDisplay(i: Int) {
        // Ensure the updated value remains within the bounds minPosition < position < maxPosition
        val newPosition = Integer.min(maxPosition, max(minPosition, selectedField + i))
        // Only update the display if the position has changed
        if (newPosition != selectedField) {
            selectedField = newPosition
            updateDisplay()
        }
    }

    private fun handleMultiLineFieldEdit(selectedElement: MultiLineField) {
        val input = MultilineEditor(terminal, session, "Editing Spark Query")
            .run(selectedElement.value)
        updateDisplay(input)
    }

    private fun handleFieldEdit(selectedElement: Field) {
        // TODO - update status line to say " editing <fieldname> | press enter to save and exit
        disableRawMode()

        val inputLength = selectedElement.value.length

        // Move the cursor to the start of the value string before we accept input since it will be in the
        // wrong place if we already have a value (we default to placing the cursor at the end of the string).
        moveCursorLeft(inputLength)

        val lineReader = LineReaderBuilder.builder()
            .terminal(this.terminal)
            .build()

        val currentValue = selectedElement.value

        // Since we need to set the prompt to a single space we need to shift the cursor left on char before
        // we accept user input otherwise the cursor will not be properly aligned.
        moveCursorLeft(1)
        // Now make the cursor visible
        showCursor()

        val input = lineReader.readLine(" ", null, currentValue)

        // Hide the cursor again as soon as the user has hit enter.
        hideCursor()

        enableRawMode()
        updateDisplay(input)
    }

    private fun handleExit() {
        // TODO - are you sure prompt
        clearDisplay()
        showCursor()
        disableRawMode()
    }

    private enum class Operation {
        EXIT,
        UP,
        DOWN,
        EDIT,
        SAVE
    }

    private fun bindKeys(): KeyMap<Operation> {
        val map = KeyMap<Operation>()
        map.bind(Operation.EXIT, "\u001B")
        map.bind(Operation.UP, "\u001B[A", "k")
        map.bind(Operation.DOWN, "\u001B[B", "j")
        map.bind(Operation.EDIT, "\r")
        map.bind(Operation.SAVE, KeyMap.ctrl('S'))

        return map
    }


}
