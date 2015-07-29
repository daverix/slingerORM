package net.daverix.slingerorm.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

class SelectSingleMethod implements StorageMethod {
    private final String methodName;
    private final String returnValue;
    private final String parameters;
    private final String where;
    private final Collection<String> whereArgs;
    private final MapperDescription mapperDescription;

    SelectSingleMethod(String methodName,
                       String returnValue,
                       String parameters,
                       String where,
                       Collection<String> whereArgs,
                       MapperDescription mapperDescription) {
        this.methodName = methodName;
        this.returnValue = returnValue;
        this.parameters = parameters;
        this.where = where;
        this.whereArgs = whereArgs;
        this.mapperDescription = mapperDescription;
    }

    @Override
    public void write(Writer writer) throws IOException {
        String args = createArguments();

        writer.write("    @Override\n");
        writer.write("    public " + returnValue + " " + methodName + "(" + parameters + ") {\n");
        writer.write("        Cursor cursor = null;\n");
        writer.write("        try {\n");
        writer.write("            cursor = db.query(false, " + mapperDescription.getVariableName() + ".getTableName(), " + mapperDescription.getVariableName() + ".getFieldNames(), \"" + where + "\", " + args + ", null, null, null, \"1\");\n");
        writer.write("            if(!cursor.moveToFirst()) return null;\n");
        writer.write("            \n");
        writer.write("            " + returnValue+ " item = new " + returnValue + "();\n");
        writer.write("            " + mapperDescription.getVariableName() + ".mapItem(cursor, item);\n");
        writer.write("            return item;\n");
        writer.write("        } finally {\n");
        writer.write("            if(cursor != null) cursor.close();\n");
        writer.write("        }\n");
        writer.write("    }\n");
        writer.write("\n");
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

    @Override
    public MapperDescription getMapper() {
        return mapperDescription;
    }
}
