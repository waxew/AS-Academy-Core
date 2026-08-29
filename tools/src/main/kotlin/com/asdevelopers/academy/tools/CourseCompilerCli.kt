package com.asdevelopers.academy.tools

import com.asdevelopers.academy.core.content.CoursePackageValidator
import com.asdevelopers.academy.core.content.DirectoryCoursePackageReader
import java.io.File
import kotlin.system.exitProcess

/**
 * ابزار رسمی Validate/Compile برای Course Repositoryها.
 *
 * ورودی پوشه‌ای برای نویسنده محتوا خوانا می‌ماند و خروجی `bundle.json` همان فایلی است که Android Loader مصرف می‌کند.
 */
fun main(arguments: Array<String>) {
    if (arguments.isEmpty() || arguments.first() in setOf("help", "--help", "-h")) {
        printUsage()
        return
    }

    val command = arguments.first()
    val sourceDirectory = arguments.getOrNull(1)?.let(::File)
        ?: fail("Course directory is required.")
    val reader = DirectoryCoursePackageReader()

    try {
        when (command) {
            "validate" -> {
                // همان Validator زمان نصب اجرا می‌شود؛ پس نتیجه CI و Android با هم اختلاف ندارد.
                val result = CoursePackageValidator().validate(reader.read(sourceDirectory))
                printResult(result.errors, result.warnings)
                if (!result.isValid) exitProcess(EXIT_VALIDATION_ERROR)
                println("Course package is valid.")
            }

            "compile" -> {
                val outputFile = arguments.getOrNull(2)?.let(::File)
                    ?: fail("Output bundle.json path is required for compile.")
                val result = reader.compile(sourceDirectory, outputFile)
                printResult(result.errors, result.warnings)
                if (!result.isValid) exitProcess(EXIT_VALIDATION_ERROR)
                println("Course package compiled: ${outputFile.absolutePath}")
            }

            else -> fail("Unknown command: $command")
        }
    } catch (error: Exception) {
        // پیام کوتاه برای CI چاپ می‌شود و Stacktrace با --stacktrace Gradle همچنان در دسترس است.
        fail(error.message ?: "Course package processing failed.")
    }
}

private fun printResult(errors: List<String>, warnings: List<String>) {
    warnings.forEach { println("WARNING: $it") }
    errors.forEach { println("ERROR: $it") }
}

private fun printUsage() {
    println("Usage:")
    println("  validate <course-directory>")
    println("  compile <course-directory> <output-bundle.json>")
}

private fun fail(message: String): Nothing {
    System.err.println("ERROR: $message")
    printUsage()
    exitProcess(EXIT_USAGE_ERROR)
}

private const val EXIT_USAGE_ERROR = 2
private const val EXIT_VALIDATION_ERROR = 3
