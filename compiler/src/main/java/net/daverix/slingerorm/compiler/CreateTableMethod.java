package net.daverix.slingerorm.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;

class CreateTableMethod implements StorageMethod {
    private final String methodName;
    private final MapperDescription mapperDescription;

    public CreateTableMethod(String methodName, MapperDescription mapperDescription) {
        this.mapperDescription = mapperDescription;
        this.methodName = methodName;
    }

    @Override
    public void write(Writer writer) throws IOException {
        if(writer == null) throw new IllegalArgumentException("writer is null");

        writer.write("    @Override\n");
        writer.write("    public void " + methodName + "(SQLiteDatabase db) {\n");
        writer.write("        if(db == null) throw new IllegalArgumentException(\"db is null\");\n");
        writer.write("\n");
        writer.write("        db.execSQL(" + mapperDescription.getVariableName() + ".createTable());\n");
        writer.write("    }\n");
        writer.write("\n");
    }

    @Override
    public Collection<String> getImports() {
        return Collections.singletonList("android.database.sqlite.SQLiteDatabase");
    }

    @Override
    public MapperDescription getMapper() {
        return mapperDescription;
    }
}
