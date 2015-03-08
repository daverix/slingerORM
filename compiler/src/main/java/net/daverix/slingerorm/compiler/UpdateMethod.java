package net.daverix.slingerorm.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

class UpdateMethod implements StorageMethod {
    private String methodName;
    private String tableName;
    private String databaseEntityTypeName;
    private String databaseEntityTypeQualifiedName;
    private Map<String,String> entityProperties;
    private String where;
    private final Collection<String> whereGetters;

    UpdateMethod(String methodName,
                 String tableName,
                 String databaseEntityTypeName,
                 String databaseEntityTypeQualifiedName,
                 Map<String, String> entityProperties,
                 String where,
                 Collection<String> whereGetters) {
        this.methodName = methodName;
        this.tableName = tableName;
        this.databaseEntityTypeName = databaseEntityTypeName;
        this.databaseEntityTypeQualifiedName = databaseEntityTypeQualifiedName;
        this.entityProperties = entityProperties;
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
        writer.write("        ContentValues values = new ContentValues();\n");

        for(String key : entityProperties.keySet()) {
            writer.write("        values.put(\"" + key + "\", " + entityProperties.get(key) + ");\n");
        }

        String whereArgs = createArguments();
        writer.write("        db.update(\"" + tableName + "\", values, \"" + where + "\", " + whereArgs + ");\n");
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
