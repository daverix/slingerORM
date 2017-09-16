package net.daverix.slingerorm.compiler


fun String.firstCharLowerCase() = substring(0, 1).toLowerCase() + substring(1)

fun String.countSqliteArgs(): Int {
    return count { c -> c == '?' }
}

fun Iterable<String>.quote(): List<String> = map { "\"$it\"" }

fun CharSequence.getPackage(): String = substring(0, lastIndexOf("."))
