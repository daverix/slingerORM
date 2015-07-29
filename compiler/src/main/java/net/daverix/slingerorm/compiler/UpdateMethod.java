package net.daverix.slingerorm.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

class UpdateMethod implements StorageMethod {
    private final String methodName;
    private final String databaseEntityTypeName;
    private final String databaseEntityTypeQualifiedName;
    private final String where;
    private final Collection<String> whereGetters;
    private final MapperDescription mapperDescription;

    UpdateMethod(String methodName,
                 String databaseEntityTypeName,
                 String databaseEntityTypeQualifiedName,
                 String where,
                 Collection<String> whereGetters,
                 MapperDescription mapperDescription) {
        this.methodName = methodName;
        this.databaseEntityTypeName = databaseEntityTypeName;
        this.databaseEntityTypeQualifiedName = databaseEntityTypeQualifiedName;
        this.where = where;
        this.whereGetters = whereGetters;
        this.mapperDescription = mapperDescription;
    }

    @Override
    public void write(Writer writer) throws IOException {
        if(writer == null) throw new IllegalArgumentException("writer is null");

        writer.write("    @Override\n");
        writer.write("    public void " + methodName + "(SQLiteDatabase db, " + databaseEntityTypeName + " item) {\n");
        writer.write("        if(db == null) throw new IllegalArgumentException(\"db is null\");\n");
        writer.write("        if(item == null) throw new IllegalArgumentException(\"entity is null\");\n");
        writer.write("\n");
        writer.write("        ContentValues values = new ContentValues();\n");
        writer.write("        " + mapperDescription.getVariableName() + ".mapValues(item, values);\n");

        String whereArgs = createArguments();
        writer.write("        db.update(" + mapperDescription.getVariableName() + ".getTableName(), values, \"" + where + "\", " + whereArgs + ");\n");
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

    @Override
    public MapperDescription getMapper() {
        return mapperDescription;
    }
}
