package de.jensklingenberg.ktorfit

import KtorfitProcessorProvider
import com.google.common.truth.Truth
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspIncremental
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import de.jensklingenberg.ktorfit.model.KtorfitError
import org.junit.Assert
import org.junit.Test
import java.io.File

class ReqBuilderAnnotationsTest {

    private val httpReqBuilderSource = SourceFile.kotlin(
        "ReqBuilder.kt", """
      package io.ktor.client.request
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.ReqBuilder

interface HttpRequestBuilder
    """
    )

    @Test
    fun whenNoRequestBuilderAnnotationsFound_KeepArgumentEmpty() {

        val source = SourceFile.kotlin(
            "Source.kt", """
      package com.example.api
import de.jensklingenberg.ktorfit.http.GET

interface TestService {

    @GET("posts")
    suspend fun test(): String
    
}
    """
        )


        val expectedFunctionText = "requestBuilder ="

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            inheritClassPath = true
            symbolProcessorProviders = listOf(KtorfitProcessorProvider())
            kspIncremental = true
        }
        val result = compilation.compile()
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val generatedSourcesDir = compilation.kspSourcesDir
        val generatedFile = File(
            generatedSourcesDir,
            "/kotlin/com/example/api/_TestServiceImpl.kt"
        )
        Truth.assertThat(generatedFile.exists()).isTrue()
        Truth.assertThat(generatedFile.readText().contains(expectedFunctionText)).isFalse()
    }


    @Test
    fun addRequestBuilderArgumentWhenAnnotationFound() {

        val source = SourceFile.kotlin(
            "Source.kt", """
      package com.example.api
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.ReqBuilder
import io.ktor.client.request.*

interface TestService {
    @GET("posts")
    suspend fun test(@ReqBuilder builder : HttpRequestBuilder.() -> Unit)
}
    """
        )


        val expected = "requestBuilder = builder"

        val compilation = KotlinCompilation().apply {
            sources = listOf(httpReqBuilderSource, source)
            inheritClassPath = true
            symbolProcessorProviders = listOf(KtorfitProcessorProvider())
            kspIncremental = true
        }
        val result = compilation.compile()
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val generatedSourcesDir = compilation.kspSourcesDir
        val generatedFile = File(
            generatedSourcesDir,
            "/kotlin/com/example/api/_TestServiceImpl.kt"
        )
        Truth.assertThat(generatedFile.exists()).isTrue()
        Truth.assertThat(generatedFile.readText().contains(expected)).isTrue()
    }

    @Test
    fun whenMultipleReqBuilderFound_ThrowCompilationError() {

        val source = SourceFile.kotlin(
            "Source.kt", """
   package com.example.api
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.ReqBuilder
import io.ktor.client.request.*

interface TestService {

    @GET("posts")
    suspend fun test(@ReqBuilder builder : HttpRequestBuilder.() -> Unit,@ReqBuilder builder2 : HttpRequestBuilder.() -> Unit)
    
}
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(httpReqBuilderSource,source)
            inheritClassPath = true
            symbolProcessorProviders = listOf(KtorfitProcessorProvider())
            kspIncremental = true
        }

        val result = compilation.compile()
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Assert.assertTrue(result.messages.contains(KtorfitError.ONLY_ONE_REQUEST_BUILDER_IS_ALLOWED))
    }

    @Test
    fun whenReqBuilderNOtReqBuilder_ThrowCompilationError() {

        val source = SourceFile.kotlin(
            "Source.kt", """
   package com.example.api
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.ReqBuilder
import io.ktor.client.request.*

interface TestService {

    @GET("posts")
    suspend fun test(@ReqBuilder builder : String)
    
}
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(httpReqBuilderSource,source)
            inheritClassPath = true
            symbolProcessorProviders = listOf(KtorfitProcessorProvider())
            kspIncremental = true
        }

        val result = compilation.compile()
        Truth.assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        Assert.assertTrue(result.messages.contains(KtorfitError.REQ_BUILDER_PARAMETER_TYPE_NEEDS_TO_BE_HTTP_REQUEST_BUILDER))
    }

}

