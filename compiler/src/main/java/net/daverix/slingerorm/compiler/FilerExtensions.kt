package net.daverix.slingerorm.compiler

import java.io.BufferedWriter
import java.io.Writer
import javax.annotation.processing.Filer

fun Filer.writeSourceFile(fileName: String, writeAction: Writer.() -> Unit) {
    val jfo = createSourceFile(fileName)
    BufferedWriter(jfo.openWriter()).use {
        it.writeAction()
    }
}
