package de.jensklingenberg.ktorfit.generator

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeVariableName
import de.jensklingenberg.ktorfit.model.ClassData
import de.jensklingenberg.ktorfit.model.KtorfitError.Companion.COULD_NOT_FIND_ANY_KTORFIT_ANNOTATIONS_IN_CLASS

/**
 * This will generate the Ktorfit.create() extension function
 */
fun generateKtorfitExtSource(
    classDataList: List<ClassData>,
    isJS: Boolean = false
): String {
    val classNameReflectionMethod = if (isJS) {
        /**
         * On JS "simpleName" is used to get class name, because qualifiedName does not exists
         */
        //https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/qualified-name.html
        "simpleName"
    } else {
        "qualifiedName"
    }

    val whenCaseStatements = classDataList.joinToString("") {
        val packageName = it.packageName
        val className = it.name
        "${packageName}.${className}::class ->{\n"+
                "${packageName}._${className}Impl(KtorfitClient(this)) as T\n"+
                "}\n"
    }

    val funSpec = FunSpec.builder("create")
        .addModifiers(KModifier.INLINE)
        .addTypeVariable(TypeVariableName("T").copy(reified = true))
        .receiver(TypeVariableName("Ktorfit"))
        .returns(TypeVariableName("T"))
        .beginControlFlow("return when(T::class){")
        .addStatement(whenCaseStatements)
        .addStatement("else ->{")
        .addStatement("throw IllegalArgumentException(\"${COULD_NOT_FIND_ANY_KTORFIT_ANNOTATIONS_IN_CLASS}\"+ T::class.$classNameReflectionMethod  )")
        .addStatement("}")
        .endControlFlow()
        .build()

    val fileSpec = FileSpec.builder("de.jensklingenberg.ktorfit", "KtorfitExt")
        .addFileComment("Generated by Ktorfit")
        .addImport("de.jensklingenberg.ktorfit.internal", "KtorfitClient")
        .addFunction(funSpec)
        .build()

    return fileSpec.toString()
}