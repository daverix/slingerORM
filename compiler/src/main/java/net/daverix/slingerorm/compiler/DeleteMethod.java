package net.daverix.slingerorm.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

class DeleteMethod implements StorageMethod {
    private final String methodName;
    private final String tableName;
    private final String databaseEntityTypeName;
    private final String databaseEntityTypeQualifiedName;
    private final String where;
    private final Collection<String> whereGetters;

    DeleteMethod(String methodName,
                 String tableName,
                 String databaseEntityTypeName,
                 String databaseEntityTypeQualifiedName,
                 String where,
                 Collection<String> whereGetters) {
        this.methodName = methodName;
        this.tableName = tableName;
        this.databaseEntityTypeName = databaseEntityTypeName;
        this.databaseEntityTypeQualifiedName = databaseEntityTypeQualifiedName;
        this.where = where;
        this.whereGetters = whereGetters;
    }

    @Override
    public void write(Writer writer) throws IOException {
        if(writer == null) throw new IllegalArgumentException("writer is null");

        writer.write("    @Override\n");
        writer.write("    public void " + methodName + "(SQLiteDatabase db, " + databaseEntityTypeName + " entity) {\n");
        writer.write("        if(db == null) throw new IllegalArgumentException(\"db is null\");\n");
        writer.write("        if(entity == null) throw new IllegalArgumentException(\"entity is null\");\n");
        writer.write("\n");

        String whereArgs = createArguments();
        writer.write("        db.delete(\"" + tableName + "\", \"" + where + "\", " + whereArgs + ");\n");
        writer.write("    }\n");
        writer.write("\n");
    }

    private String createArguments() {
        StringBuilder builder = new StringBuilder("new String[] {\n");
        int i=0;
        for(String getter : whereGetters) {
            builder.append("            String.valueOf(").append(getter).append(")");

            if(i < whereGetters.size()-1) {
                builder.append(",\n");
            }
            else {
                builder.append("\n");
            }
            i++;
        }
        builder.append("        }");
        return builder.toString();
    }

    @Override
    public Collection<String> getImports() {
        return Arrays.asList("android.database.sqlite.SQLiteDatabase",
                "android.content.ContentValues",
                databaseEntityTypeQualifiedName);
    }
}
