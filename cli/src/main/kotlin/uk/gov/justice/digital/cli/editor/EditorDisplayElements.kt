package uk.gov.justice.digital.cli.editor

import kotlin.math.max

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
    // TODO - handle value length exceeding available width
    override fun render(width: Int): String {
        val formattedName = String.format("%-${margin}s",  name)
        val padding = " ".repeat(width - margin - value.length - 6)
        return if (selected) "@|bold  $formattedName |@│ @|underline $value$padding |@ "
        else "@|bold,faint  $formattedName |@│ $value$padding  "

    }
}
data class MultiLineField(val name: String,
                          val value: String,
                          val selected: Boolean = false,
                          val margin: Int = name.length): Element {
    override val canSelect = true
    // TODO - handle value length exceeding available width
    override fun render(width: Int): String {
        val formattedName = String.format("%-${margin}s",  name)
        val firstLineOfValue = value.split("\n").firstOrNull()?.take(width - margin - 6) ?: ""
        val padding = " ".repeat(width - margin - firstLineOfValue.length - 6)
        return if (selected) "@|bold  $formattedName |@│ @|underline $firstLineOfValue$padding |@ "
        else "@|bold,faint  $formattedName |@│ $firstLineOfValue$padding  "

    }
}

data class Heading(val heading: String, val color: String, val backgroundColor: String? = null): Element {
    override val canSelect = false
    override fun render(width: Int): String {
        val bgString = backgroundColor?.let { ",bg($it)" } ?: ""
        val padding = " ".repeat(max(width - heading.length - 1, 0))
        return "@|fg($color)$bgString,bold  ${heading.take(width - 1)}$padding|@"
    }
}
