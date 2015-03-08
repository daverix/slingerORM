package net.daverix.slingerorm.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

class SelectSingleMethod implements StorageMethod {
    private final String methodName;
    private final String tableName;
    private final String returnValue;
    private final String parameters;
    private final String where;
    private final Collection<String> whereArgs;

    SelectSingleMethod(String methodName,
                       String tableName,
                       String returnValue,
                       String parameters,
                       String where,
                       Collection<String> whereArgs) {
        this.methodName = methodName;
        this.tableName = tableName;
        this.returnValue = returnValue;
        this.parameters = parameters;
        this.where = where;
        this.whereArgs = whereArgs;
    }

    @Override
    public void write(Writer writer) throws IOException {
        String columns = createColumns();
        String args = createArguments();

        writer.write("    @Override\n");
        writer.write("    public " + returnValue + " " + methodName + "(" + parameters + ") {\n");
        writer.write("        Cursor cursor = null;\n");
        writer.write("        try {\n");
        writer.write("            cursor = db.query(false, \"" + tableName + "\", " + columns + ", \"" + where + "\", " + args + ", null, null, null, \"1\");\n");
        writer.write("            return null;\n");
        writer.write("        } finally {\n");
        writer.write("            if(cursor != null) cursor.close();\n");
        writer.write("        }\n");
        writer.write("    }\n");
        writer.write("\n");
    }

    private String createColumns() {
        return "null";
    }

    private String createArguments() {
        return "new String[]{}";
    }

    @Override
    public Collection<String> getImports() {
        return Arrays.asList(
                "android.database.Cursor",
                "android.database.sqlite.SQLiteDatabase"
        );
    }
}
