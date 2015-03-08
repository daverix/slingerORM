package net.daverix.slingerorm.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

public class SelectMultipleMethod implements StorageMethod {
    private final String methodName;
    private final String tableName;
    private final String returnTypeName;
    private final String parameterText;
    private final String where;
    private final Collection<String> parameterGetters;
    private final String orderBy;

    public SelectMultipleMethod(String methodName,
                                String tableName,
                                String returnTypeName,
                                String parameterText,
                                String where,
                                Collection<String> parameterGetters, String orderBy) {
        this.methodName = methodName;
        this.tableName = tableName;
        this.returnTypeName = returnTypeName;
        this.parameterText = parameterText;
        this.where = where;
        this.parameterGetters = parameterGetters;
        this.orderBy = orderBy;
    }

    @Override
    public void write(Writer writer) throws IOException {
        String columns = createColumns();
        String args = createArguments();
        String orderByText = createOrderBy();

        writer.write("    @Override\n");
        writer.write("    public " + returnTypeName + " " + methodName + "(" + parameterText + ") {\n");
        writer.write("        Cursor cursor = null;\n");
        writer.write("        try {\n");
        writer.write("            cursor = db.query(false, \"" + tableName + "\", " + columns + ", \"" + where + "\", " + args + ", null, null, " + orderByText + ", null);\n");
        writer.write("            return null;\n");
        writer.write("        } finally {\n");
        writer.write("            if(cursor != null) cursor.close();\n");
        writer.write("        }\n");
        writer.write("    }\n");
        writer.write("\n");
    }

    private String createOrderBy() {
        if(orderBy == null) return "null";

        return "\"" + orderBy + "\"";
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
                "android.database.sqlite.SQLiteDatabase",
                "java.util.List",
                "java.util.ArrayList"
        );
    }
}
