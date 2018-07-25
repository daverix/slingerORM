package net.daverix.slingerorm.compiler

import com.squareup.javapoet.ClassName


object ClassNames {
    val CONTENT_VALUES: ClassName = ClassName.get("android.content", "ContentValues")
    val SQLITE_DATABASE: ClassName = ClassName.get("android.database.sqlite","SQLiteDatabase")
    val CURSOR: ClassName = ClassName.get("android.database","Cursor")
}