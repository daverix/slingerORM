package net.daverix.slingerorm.compiler.mapping.cursor;


import net.daverix.slingerorm.compiler.CursorType;
import net.daverix.slingerorm.compiler.mapping.Getter;

import java.util.Locale;

public class DefaultCursorGetter implements Getter {
    private final CursorType cursorType;
    private final String columnName;

    public DefaultCursorGetter(CursorType cursorType, String columnName) {
        this.cursorType = cursorType;
        this.columnName = columnName;
    }

    @Override
    public String get(String cursorVariableName) {
        if (cursorType == CursorType.BOOLEAN) {
            return String.format(Locale.ENGLISH,
                    "%s.getShort(%s) == 1",
                    cursorVariableName, columnName);
        }
        return String.format(Locale.ENGLISH,
                "%s.get%s(%s.getColumnIndex(\"%s\"))",
                cursorVariableName,
                getStringRepresentation(cursorType),
                cursorVariableName,
                columnName);
    }

    private String getStringRepresentation(CursorType cursorType) {
        switch (cursorType) {
            case SHORT:
                return "Short";
            case INT:
                return "Int";
            case LONG:
                return "Long";
            case FLOAT:
                return "Float";
            case DOUBLE:
                return "Double";
            case STRING:
                return "String";
            case BYTE_ARRAY:
                return "Blob";
        }
        return null;
    }
}
