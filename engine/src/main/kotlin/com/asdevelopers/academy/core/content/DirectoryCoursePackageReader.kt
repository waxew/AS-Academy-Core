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
import com.asdevelopers.academy.course.model.Flashcard
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
        // نبود فایل‌های اصلی خطای واضح تولید می‌کند و Release ناقص ساخته نمی‌شود.
        require(root.isDirectory) { "Course package root is not a directory: ${root.path}" }
        val manifest = root.requiredJson<CourseManifest>("manifest.json")
        val branding = root.requiredJson<CourseBranding>("branding.json")
        val levels = root.requiredJson<List<CourseLevel>>("levels.json")
        val chapters = root.requiredJson<List<Chapter>>("chapters.json")
        // تمام Collection directoryها object یا array را می‌پذیرند تا Fix شاخه main و Authoring ماژولار Flashcard هر دو حفظ شوند.
        val lessons = root.decodeDirectory<Lesson>("lessons")
        val quizzes = root.decodeDirectory<Quiz>("quizzes")
        val exercises = root.decodeDirectory<Exercise>("exercises")
        val projects = root.decodeDirectory<LearningProject>("projects")
        val glossary = root.decodeDirectory<GlossaryEntry>("glossary")
        val assets = root.optionalJson<List<CourseAsset>>("assets.json").orEmpty()
        val references = root.optionalJson<List<CourseReference>>("references.json").orEmpty()
        val flashcards = root.decodeDirectory<Flashcard>("flashcards")

        // Named arguments جلوی شکست‌های آینده هنگام افزودن فیلدهای optional جدید به CourseBundle را می‌گیرد.
        return CourseBundle(
            manifest = manifest,
            branding = branding,
            levels = levels,
            chapters = chapters,
            lessons = lessons,
            quizzes = quizzes,
            exercises = exercises,
            projects = projects,
            glossary = glossary,
            assets = assets,
            references = references,
            flashcards = flashcards
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
     * این قرارداد هم Courseهای بزرگ را برای فایل‌های تجمیعی/تفکیک‌شده آزاد می‌گذارد و هم Glossary/Flashcard را ماژولار نگه می‌دارد.
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
