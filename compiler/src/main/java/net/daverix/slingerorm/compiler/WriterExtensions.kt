package net.daverix.slingerorm.compiler

import java.io.Writer

fun Writer.writeLine(text: String = "") = write("$text\n")

fun <T> Writer.writeLines(lines: Iterable<T>, transform: (T) -> String) {
    lines.forEach { writeLine(transform(it)) }
}

fun <T> Writer.writeLines(lines: Iterable<T>, transform: (Int, T) -> String) {
    lines.forEachIndexed { index, item -> writeLine(transform(index, item)) }
}
