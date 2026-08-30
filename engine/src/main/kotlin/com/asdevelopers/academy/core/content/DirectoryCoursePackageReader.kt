package com.asdevelopers.academy.core.content

import com.asdevelopers.academy.core.exercise.Exercise
import com.asdevelopers.academy.core.project.LearningProject
import com.asdevelopers.academy.core.quiz.Quiz
import com.asdevelopers.academy.course.model.Chapter
import com.asdevelopers.academy.course.model.CourseAsset
import com.asdevelopers.academy.course.model.CourseBranding
import com.asdevelopers.academy.course.model.CourseLevel
import com.asdevelopers.academy.course.model.CourseManifest
import com.asdevelopers.academy.course.model.CourseReference
import com.asdevelopers.academy.course.model.GlossaryEntry
import com.asdevelopers.academy.course.model.Lesson
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Reader ابزار تولید محتوا، ساختار پوشه‌ای قابل ویرایش را به Bundle قابل نصب تبدیل می‌کند.
 */
class DirectoryCoursePackageReader(
    private val json: Json = CoursePackageCodec.defaultJson
) {
    fun read(root: File): CourseBundle {
        require(root.isDirectory) { "Course package root is not a directory: ${root.path}" }
        val manifest = root.requiredJson<CourseManifest>("manifest.json")
        val branding = root.requiredJson<CourseBranding>("branding.json")
        val levels = root.requiredJson<List<CourseLevel>>("levels.json")
        val chapters = root.requiredJson<List<Chapter>>("chapters.json")
        val lessons = root.decodeDirectory<Lesson>("lessons")
        val quizzes = root.decodeDirectory<Quiz>("quizzes")
        val exercises = root.decodeDirectory<Exercise>("exercises")
        val projects = root.decodeDirectory<LearningProject>("projects")
        val glossary = root.optionalJson<List<GlossaryEntry>>("glossary/glossary.json").orEmpty()
        val assets = root.optionalJson<List<CourseAsset>>("assets.json").orEmpty()
        val references = root.optionalJson<List<CourseReference>>("references.json").orEmpty()
        return CourseBundle(
            manifest, branding, levels, chapters, lessons, quizzes, exercises, projects, glossary, assets, references
        )
    }

    /** Directory را پس از Validation به فایل bundle.json قابل مصرف Android تبدیل می‌کند. */
    fun compile(root: File, output: File): ValidationResult {
        val bundle = read(root)
        val result = CoursePackageValidator().validate(bundle)
        if (result.isValid) {
            output.parentFile?.mkdirs()
            output.writeText(CoursePackageCodec(json).encode(bundle), Charsets.UTF_8)
        }
        return result
    }

    private inline fun <reified T> File.requiredJson(relativePath: String): T {
        val file = resolve(relativePath)
        require(file.isFile) { "Required course file is missing: $relativePath" }
        return json.decodeFromString(file.readText(Charsets.UTF_8))
    }

    private inline fun <reified T> File.optionalJson(relativePath: String): T? {
        val file = resolve(relativePath)
        return file.takeIf(File::isFile)
            ?.readText(Charsets.UTF_8)
            ?.let { rawJson -> json.decodeFromString<T>(rawJson) }
    }

    /**
     * هر فایل داخل پوشه می‌تواند یک آیتم مستقل یا آرایه‌ای از همان آیتم‌ها باشد.
     * این کار Courseهای بزرگ را برای نگهداری فایل‌های تجمیعی یا تفکیک‌شده آزاد می‌گذارد.
     */
    private inline fun <reified T> File.decodeDirectory(relativePath: String): List<T> {
        val directory = resolve(relativePath)
        if (!directory.isDirectory) return emptyList()
        return directory.walkTopDown()
            .filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
            .sortedBy { it.relativeTo(directory).invariantSeparatorsPath }
            .flatMap { file ->
                val raw = file.readText(Charsets.UTF_8).trim()
                when {
                    raw.startsWith("[") -> json.decodeFromString<List<T>>(raw).asSequence()
                    raw.startsWith("{") -> sequenceOf(json.decodeFromString<T>(raw))
                    else -> throw IllegalArgumentException(
                        "Unsupported JSON root in ${file.relativeTo(this).invariantSeparatorsPath}; expected object or array"
                    )
                }
            }
            .toList()
    }
}
