package net.daverix.slingerorm.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

class CreateTableMethod implements StorageMethod {
    private final String methodName;
    private final String createTableSql;

    public CreateTableMethod(String methodName, String createTableSql) {
        if(methodName == null) throw new IllegalArgumentException("methodName is null");
        if(createTableSql == null) throw new IllegalArgumentException("createTableSql is null");

        this.methodName = methodName;
        this.createTableSql = createTableSql;
    }

    @Override
    public void write(Writer writer) throws IOException {
        if(writer == null) throw new IllegalArgumentException("writer is null");

        writer.write("    @Override\n");
        writer.write("    public void " + methodName + "(SQLiteDatabase db) {\n");
        writer.write("        if(db == null) throw new IllegalArgumentException(\"db is null\");\n");
        writer.write("\n");
        writer.write("        db.execSQL(\"" + createTableSql + "\");\n");
        writer.write("    }\n");
        writer.write("\n");
    }

    @Override
    public Collection<String> getImports() {
        return Arrays.asList("android.database.sqlite.SQLiteDatabase");
    }
}
