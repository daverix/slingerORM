package net.daverix.slingerorm.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

interface StorageMethod {
    void write(Writer writer) throws IOException;

    Collection<String> getImports();

    MapperDescription getMapper();
}
