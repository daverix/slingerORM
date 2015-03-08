package net.daverix.slingerorm.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

class InsertMethod implements StorageMethod {
    private final String methodName;
    private String databaseEntityTypeName;
    private final String databaseEntityTypeQualifiedName;
    private final String tableName;
    private final Map<String, String> entityProperties;

    public InsertMethod(String methodName,
                        String databaseEntityTypeName,
                        String databaseEntityTypeQualifiedName,
                        String tableName,
                        Map<String,String> entityProperties) {
        this.methodName = methodName;
        this.databaseEntityTypeName = databaseEntityTypeName;
        this.databaseEntityTypeQualifiedName = databaseEntityTypeQualifiedName;
        this.tableName = tableName;
        this.entityProperties = entityProperties;
    }

    @Override
    public void write(Writer writer) throws IOException {
        writer.write("    @Override\n");
        writer.write("    public void " + methodName + "(SQLiteDatabase db, " + databaseEntityTypeName + " entity) {\n");
        writer.write("        if(db == null) throw new IllegalArgumentException(\"db is null\");\n");
        writer.write("        if(entity == null) throw new IllegalArgumentException(\"entity is null\");\n");
        writer.write("\n");
        writer.write("        ContentValues values = new ContentValues();\n");

        for(String key : entityProperties.keySet()) {
            writer.write("        values.put(\"" + key + "\", " + entityProperties.get(key) + ");\n");
        }

        writer.write("        db.insertOrThrow(\"" + tableName + "\", null, values);\n");
        writer.write("    }\n");
        writer.write("\n");
    }

    @Override
    public Collection<String> getImports() {
        return Arrays.asList("android.database.sqlite.SQLiteDatabase",
                "android.content.ContentValues",
                databaseEntityTypeQualifiedName);
    }
}
