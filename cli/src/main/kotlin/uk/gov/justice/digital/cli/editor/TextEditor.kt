package uk.gov.justice.digital.cli.editor

import org.jline.keymap.BindingReader
import org.jline.keymap.KeyMap
import org.jline.utils.InfoCmp.Capability
import uk.gov.justice.digital.cli.editor.TextEditor.Operation.*
import uk.gov.justice.digital.cli.session.InteractiveSession
import kotlin.math.min

/**
 * A simple text editor that provides enough functionality to allow a user to enter sql queries, tags etc..
 *
 * There are some limitations with regard to how text wrapping is handled as follows
 *   o pressing enter does not insert a new line or break the current line at the cursor location
 *   o deletion will only move any trailing text up to the next line if there is space for the entire string
 *   o maximum number of lines hardcoded to 18 lines (this can easily be made dynamic if required)
 */
class TextEditor(private val session: InteractiveSession, private val heading: String) {

    private val keyReaderTimeout = 250L

    private val terminal = session.terminal()

    // Editor bounds (except width) hard coded to fit within the constraints of a default 80 x 24 terminal session.
    private val lineOffset = 4 // Top three lines of the screen are not editable
    // Editor bounds. Can be made dynamic by taking width/height from the terminal.
    private val minLine = 0
    private val maxLine = 18 // Top 4 and bottom 2 lines reserved for heading and status lines
    private val minColumn = 0
    private val maxColumn = terminal.width

    private val tabLength = 4 // Tabs will be converted into this number of spaces

    // Initial empty text area to be edited.
    private val lines = Array(maxLine) { _ -> "" }.toMutableList()

    // Current cursor position where currentLine is relative to lineOffset.
    private var currentLine = 0
    private var currentColumn = 0

    fun run(text: String? = null): String {

        val originalAttributes = terminal.enterRawMode()

        terminal.showCursor()
        terminal.flush()

        terminal.clearDisplay()
        terminal.flush()

        val padding = " ".repeat(terminal.width - heading.length - 1)

        println()
        println(session.toAnsi("@|fg(black),bg(cyan),bold  $heading$padding|@"))
        println()

        val editReader = BindingReader(terminal.reader())

        val map = bindKeys()

        // Populate lines with existing data where defined
        text?.let {
            text.split("\n")
                .withIndex()
                .forEach { lines[it.index] = it.value }
        }

        lines.forEach { println(it) }

        terminal.moveCursorUp(lines.size)

        terminal.flush()

        terminal.saveCursorPosition()
        terminal.moveCursorTo(23, 0)

        val statusLine = "keys │ ↑ move up │ ↓ move down │ ← move left │ → move right │ CTRL-W save │ ESC exit"
            .take(terminal.width - 2)
        val statusPadding = " ".repeat(terminal.width - min(statusLine.length, terminal.width) - 1)
        print(session.toAnsi("@|fg(black),bg(white),bold  $statusLine$statusPadding|@"))

        terminal.restoreSavedCursorPosition()
        terminal.showCursor()
        terminal.flush()

        while(true) {
            when (editReader.readBinding(map, null, true)) {
                UP      -> handleUp()
                DOWN    -> handleDown()
                LEFT    -> handleLeft()
                RIGHT   -> handleRight()
                INSERT  -> handleInsert(editReader.lastBinding)
                ENTER   -> handleEnter()
                DELETE  -> handleDelete()
                EXIT    -> return text ?: ""
                ACCEPT  -> break
                else    -> terminal.bell()
            }
            terminal.flush()
        }

        // Restore original terminal settings which will disable raw mode.
        terminal.attributes = originalAttributes

        return lines.filter { it.isNotEmpty() }.joinToString("\n")
    }

    private fun handleUp() {
        if (onFirstLine()) terminal.bell()
        else handleVerticalMovement(-1)
    }

    private fun handleDown() {
        if (onLastPopulatedLine()) terminal.bell()
        else handleVerticalMovement(1)
    }

    private fun handleVerticalMovement(offset: Int) {
        currentLine += offset
        // When moving from a longer line to a shorter line, ensure the cursor is at the end of the shorter line.
        currentColumn = Integer.min(currentColumn, lines[currentLine].length)
        terminal.moveCursorTo(currentLine + lineOffset, currentColumn + 1)
    }

    private fun handleLeft() {
        if (currentColumn > minColumn) {
            terminal.moveCursorLeft(1)
            currentColumn--
        }
        else terminal.bell()
    }

    private fun handleRight() {
        if (atEndOfText() || atEndOfText()) terminal.bell()
        else {
            terminal.moveCursorRight(1)
            currentColumn++
        }
    }

    private fun handleInsert(lastBinding: String) {
        // Remap tab to spaces
        val input = if (lastBinding == "\t") " ".repeat(tabLength) else lastBinding
        input.forEach { c ->
            if (atEndOfLine()) {
                if (canAddNewLine()) addNewLineStartingWithCharacter(c)
                else terminal.bell()
            }
            else {
                if (atEndOfText()) appendCharacterToCurrentLine(c)
                else insertCharacterAtCurrentPosition(c)
            }
        }
    }

    private fun handleEnter() {
        if (canAddNewLine()) {
            terminal.moveCursorDown(1)
            print("\r")
            currentColumn = 0
            currentLine++
        }
        else terminal.bell()
    }

    private fun handleDelete() {
        if (lines[currentLine].isNotEmpty() && !atStartOfLine()) deleteCharacterAtCurrentPosition()
        else if (!onFirstLine() && currentLineCanFitOnLineAbove()) moveCurrentLineToEndOfPreviousLine()
        else terminal.bell() // Cannot much current line content to line above - not enough space.
    }

    private fun deleteCharacterAtCurrentPosition() {
        val line = lines[currentLine]
        lines[currentLine] = line.removeRange(currentColumn - 1, currentColumn)
        terminal.moveCursorLeft(1)
        terminal.puts(Capability.delete_character)
        currentColumn--
    }

    private fun moveCurrentLineToEndOfPreviousLine() {
        val line = lines[currentLine]

        // Clear the current line since the contents will be moved up to the end of line above.
        lines[currentLine] = ""
        terminal.clearLine()

        currentLine--

        // Move the cursor up to the end of the line above.
        terminal.moveCursorUp(1)
        terminal.moveCursorRight(lines[currentLine].length)
        currentColumn = lines[currentLine].length

        // Print the contents of the line we started on and move the cursor back to where it was...
        print(line)
        terminal.moveCursorLeft(line.length)
        // ...and append the original line to the end of the current line.
        lines[currentLine] = lines[currentLine] + line
    }

    private fun addNewLineStartingWithCharacter(c: Char) {
        terminal.moveCursorDown(1)
        print("\r")
        currentLine++
        lines[currentLine] = c + lines[currentLine]
        print(lines[currentLine])
        terminal.moveCursorLeft(lines[currentLine].length - 1)
        terminal.flush()
        currentColumn = 1
    }

    private fun appendCharacterToCurrentLine(c: Char) {
        lines[currentLine] = lines[currentLine] + c
        print(c)
        currentColumn++
    }

    private fun insertCharacterAtCurrentPosition(c: Char) {
        val line = lines[currentLine]
        val charsRemaining = line.substring(currentColumn).length
        lines[currentLine] = line.substring(0, currentColumn) +
            c +
            line.substring(currentColumn)
        print(c)
        print(line.substring(currentColumn))
        currentColumn++

        terminal.moveCursorLeft(charsRemaining)
    }

    // Predicate methods defining various tests around the context of the lines array aka the buffer.
    private fun onFirstLine(): Boolean = currentLine == minLine
    private fun onLastLine(): Boolean = currentLine == maxLine - 1
    private fun onLastPopulatedLine(): Boolean = currentLine == (lines.indexOfLast { it.isNotEmpty() }) || bufferIsEmpty()
    private fun atStartOfLine(): Boolean = currentColumn == 0
    private fun atEndOfLine(): Boolean = currentColumn == maxColumn - 1
    private fun atEndOfText(): Boolean = currentColumn == lines[currentLine].length
    private fun bufferIsEmpty(): Boolean = lines.indexOfFirst { it.isEmpty() } == 0
    private fun bufferIsFull(): Boolean = lines.last().isNotEmpty()
    private fun canAddNewLine(): Boolean = !onLastLine() && !bufferIsFull()
    // Test whether the line above has space for the text remaining on the current line.
    private fun currentLineCanFitOnLineAbove(): Boolean = lines[currentLine].length + lines[currentLine-1].length < terminal.width

    enum class Operation {
        UP,
        DOWN,
        RIGHT,
        LEFT,
        INSERT,
        ENTER,
        DELETE,
        EXIT,
        ACCEPT,
    }

    private fun bindKeys(): KeyMap<Operation> {
        val map = KeyMap<Operation>()
        map.bind(UP, "\u001B[A")
        map.bind(DOWN, "\u001B[B")
        map.bind(RIGHT, "\u001B[C")
        map.bind(LEFT, "\u001B[D")
        map.bind(ENTER, "\r")
        map.bind(DELETE, "\b")
        map.bind(EXIT, "\u001B")
        map.bind(ACCEPT, KeyMap.ctrl('W'))

        // Allow user to enter characters which are passed to the insert handler.
        for (i in 32..255) {
            // Only bind to insert if the char is not DEL (ASCII 127)
            if (i != 127) map.bind(INSERT, Character.toString(i))
            else map.bind(DELETE, Character.toString(i))
        }

        // Also bind the tab character to insert
        map.bind(INSERT, Character.toString(9))

        // Set a shorter timeout for ambiguous keys so hitting ESC to exit is more responsive
        map.ambiguousTimeout = keyReaderTimeout

        return map
    }

}