package com.asdevelopers.academy.core.content

import com.asdevelopers.academy.course.model.Chapter
import com.asdevelopers.academy.course.model.CourseLevel
import com.asdevelopers.academy.course.model.CourseManifest
import com.asdevelopers.academy.course.model.Lesson

/** نتیجه اعتبارسنجی Package قبل از Import یا Release. */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

/**
 * Validator مرکزی Course Package.
 * تمام اپ‌ها باید همین Validator را استفاده کنند تا قوانین محتوا بین پروژه‌ها متفاوت نشود.
 */
class CoursePackageValidator {

    fun validate(
        manifest: CourseManifest,
        levels: List<CourseLevel>,
        chapters: List<Chapter>,
        lessons: List<Lesson>
    ): ValidationResult {
        val errors = mutableListOf<String>()

        if (manifest.courseId.isBlank()) errors += "courseId cannot be blank"
        if (manifest.version.isBlank()) errors += "course version cannot be blank"
        if (manifest.contentSchemaVersion <= 0) errors += "contentSchemaVersion must be positive"

        // Stable IDها باید در محدوده نوع خود یکتا باشند.
        checkUniqueIds("level", levels.map { it.id }, errors)
        checkUniqueIds("chapter", chapters.map { it.id }, errors)
        checkUniqueIds("lesson", lessons.map { it.id }, errors)

        val levelIds = levels.map { it.id }.toSet()
        val chapterIds = chapters.map { it.id }.toSet()

        chapters.filter { it.levelId !in levelIds }
            .forEach { errors += "chapter ${it.id} references missing level ${it.levelId}" }

        lessons.filter { it.chapterId !in chapterIds }
            .forEach { errors += "lesson ${it.id} references missing chapter ${it.chapterId}" }

        lessons.forEach { lesson ->
            if (lesson.title.isBlank()) errors += "lesson ${lesson.id} has an empty title"
            if (lesson.blocks.isEmpty()) errors += "lesson ${lesson.id} has no content blocks"
            checkUniqueIds("block in ${lesson.id}", lesson.blocks.map { it.id }, errors)
        }

        return ValidationResult(isValid = errors.isEmpty(), errors = errors)
    }

    private fun checkUniqueIds(label: String, ids: List<String>, errors: MutableList<String>) {
        ids.groupingBy { it }.eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { errors += "duplicate $label id: $it" }
    }
}
