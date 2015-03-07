package net.daverix.slingerorm.compiler;

import java.io.Writer;

interface StorageMethod {
    String getQualifiedName();

    void write(Writer writer);
}
