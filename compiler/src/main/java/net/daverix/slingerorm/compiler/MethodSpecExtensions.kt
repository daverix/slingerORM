package net.daverix.slingerorm.compiler

import com.squareup.javapoet.MethodSpec
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement


inline fun ExecutableElement.toMethodSpec(spec: MethodSpec.Builder.() -> Unit): MethodSpec {
    val builder = MethodSpec.overriding(this)
    spec(builder)
    return builder.build()
}

inline fun MethodSpec.Builder.tryFinally(tryBlock: MethodSpec.Builder.() -> Unit,
                                         finallyBlock: MethodSpec.Builder.() -> Unit) {
    beginControlFlow("try")
    tryBlock(this)
    nextControlFlow("finally")
    finallyBlock(this)
    endControlFlow()
}

fun MethodSpec.Builder.addVariableArrayCode(variableElements: List<VariableElement>, indent: String="") {
    variableElements.forEachIndexed { index, paramElement ->
        val paramName = paramElement.getParameterVariable()

        addCode("$indent$paramName")
        if(index < variableElements.size - 1)
            addCode(",")

        addCode("\n")
    }
}
