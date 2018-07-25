package net.daverix.slingerorm.compiler


fun String.firstCharLowerCase() = first().toLowerCase() + substring(1)

fun String.countSqliteArgs(): Int {
    return count { c -> c == '?' }
}

fun String.encloseStringValueOfIfNotString(isString: Boolean): String =
        if (isString) this else "String.valueOf(${this})"

fun String.fromCamelCaseToScreamingSnakeCase(): String {
    val builder = StringBuilder()
    for(i in 0 until length) {
        if(i > 0 && i < length-1 && this[i].isUpperCase())
            builder.append("_")

        builder.append(this[i].toUpperCase())
    }
    return builder.toString()
}