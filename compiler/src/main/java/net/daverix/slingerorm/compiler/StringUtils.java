package net.daverix.slingerorm.compiler;

public class StringUtils {
    public static String lowerCaseFirstCharacter(String input) {
        if(input == null) throw new IllegalArgumentException("input is null");
        if(input.length() < 2) throw new IllegalArgumentException("input is to small");

        return input.substring(0, 1).toLowerCase() + input.substring(1);
    }
}
