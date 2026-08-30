package com.asdevelopers.academy.core.content

import com.asdevelopers.academy.core.exercise.ExerciseType
import com.asdevelopers.academy.core.quiz.QuestionType
import com.asdevelopers.academy.core.version.CoreVersion
import com.asdevelopers.academy.core.version.SemanticVersion
import com.asdevelopers.academy.course.model.LessonBlockType

/** نتیجه Validation قبل از نصب یا Build دوره. */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String> = emptyList()
)

/**
 * Validator مرکزی؛ همان Ruleها باید در CI و Runtime استفاده شوند تا Package خراب هرگز نصب نشود.
 */
class CoursePackageValidator {
    private val stableIdPattern = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
    private val hexColorPattern = Regex("^#[0-9A-Fa-f]{6}$")
    private val sha256Pattern = Regex("^[0-9A-Fa-f]{64}$")
    private val allowedSchemes = setOf("https", "http")

    fun validate(bundle: CourseBundle): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val manifest = bundle.manifest

        validateManifest(bundle, errors)
        checkIds("level", bundle.levels.map { it.id }, errors)
        checkIds("chapter", bundle.chapters.map { it.id }, errors)
        checkIds("lesson", bundle.lessons.map { it.id }, errors)
        checkIds("quiz", bundle.quizzes.map { it.id }, errors)
        checkIds("exercise", bundle.exercises.map { it.id }, errors)
        checkIds("project", bundle.projects.map { it.id }, errors)
        checkIds("glossary", bundle.glossary.map { it.id }, errors)
        checkIds("asset", bundle.assets.map { it.id }, errors)
        checkIds("reference", bundle.references.map { it.id }, errors)
        checkIds("flashcard", bundle.flashcards.map { it.id }, errors)

        val levelIds = bundle.levels.map { it.id }.toSet()
        val chapterIds = bundle.chapters.map { it.id }.toSet()
        val lessonIds = bundle.lessons.map { it.id }.toSet()
        val assetIds = bundle.assets.map { it.id }.toSet()

        bundle.levels.forEach { level ->
            if (level.courseId != manifest.courseId) errors += "level ${level.id} belongs to ${level.courseId}, not ${manifest.courseId}"
            if (level.title.isBlank()) errors += "level ${level.id} has an empty title"
            if (level.order < 0) errors += "level ${level.id} has a negative order"
        }
        checkUniqueOrder("level", bundle.levels.groupBy { it.courseId }, { it.order }, errors)

        bundle.chapters.forEach { chapter ->
            if (chapter.levelId !in levelIds) errors += "chapter ${chapter.id} references missing level ${chapter.levelId}"
            if (chapter.title.isBlank()) errors += "chapter ${chapter.id} has an empty title"
            if (chapter.order < 0) errors += "chapter ${chapter.id} has a negative order"
            chapter.prerequisites.filter { it !in chapterIds }.forEach {
                errors += "chapter ${chapter.id} references missing prerequisite chapter $it"
            }
        }
        checkUniqueOrder("chapter", bundle.chapters.groupBy { it.levelId }, { it.order }, errors)

        bundle.lessons.forEach { lesson ->
            if (lesson.chapterId !in chapterIds) errors += "lesson ${lesson.id} references missing chapter ${lesson.chapterId}"
            if (lesson.title.isBlank()) errors += "lesson ${lesson.id} has an empty title"
            if (lesson.estimatedMinutes <= 0) errors += "lesson ${lesson.id} estimatedMinutes must be positive"
            if (lesson.blocks.isEmpty()) errors += "lesson ${lesson.id} has no content blocks"
            if (lesson.order < 0) errors += "lesson ${lesson.id} has a negative order"
            checkIds("block in ${lesson.id}", lesson.blocks.map { it.id }, errors)
            lesson.prerequisites.filter { it !in lessonIds }.forEach {
                errors += "lesson ${lesson.id} references missing prerequisite $it"
            }
            lesson.blocks.forEach { block ->
                if (block.content.isBlank() && block.metadata["assetId"].isNullOrBlank()) {
                    errors += "block ${block.id} in ${lesson.id} has neither content nor assetId"
                }
                block.metadata["assetId"]?.takeIf { it !in assetIds }?.let {
                    errors += "block ${block.id} references missing asset $it"
                }
                // Aliasهای قدیمی خوانده می‌شوند تا Update کاربران خراب نشود، ولی Content جدید باید نام canonical را تولید کند.
                if (block.type == LessonBlockType.EXERCISE_LINK) {
                    warnings += "block ${block.id} uses legacy EXERCISE_LINK; use EXERCISE"
                }
                if (block.type == LessonBlockType.PROJECT) {
                    warnings += "block ${block.id} uses legacy PROJECT; use PROJECT_LINK"
                }
            }
        }
        checkUniqueOrder("lesson", bundle.lessons.groupBy { it.chapterId }, { it.order }, errors)

        validateQuizzes(bundle, lessonIds, chapterIds, errors)
        validateExercises(bundle, lessonIds, errors)
        validateProjects(bundle, lessonIds, errors)
        validateResources(bundle, lessonIds, errors)
        validateFlashcards(bundle, lessonIds, errors)

        // Capability روشن ولی محتوای خالی معمولاً اشتباه Package است، اما انتشار آزمایشی را مسدود نمی‌کند.
        if (manifest.capabilities.quizzes && bundle.quizzes.isEmpty()) warnings += "quizzes capability is enabled but no quiz exists"
        if (manifest.capabilities.exercises && bundle.exercises.isEmpty()) warnings += "exercises capability is enabled but no exercise exists"
        if (manifest.capabilities.projects && bundle.projects.isEmpty()) warnings += "projects capability is enabled but no project exists"
        if (manifest.capabilities.glossary && bundle.glossary.isEmpty()) warnings += "glossary capability is enabled but glossary is empty"
        if (manifest.capabilities.flashcards && bundle.flashcards.isEmpty()) warnings += "flashcards capability is enabled but no flashcard exists"

        return ValidationResult(errors.isEmpty(), errors.distinct(), warnings.distinct())
    }

    /** API قدیمی Validator برای مصرف‌کننده‌های اولیه حفظ شده است. */
    fun validate(
        manifest: com.asdevelopers.academy.course.model.CourseManifest,
        levels: List<com.asdevelopers.academy.course.model.CourseLevel>,
        chapters: List<com.asdevelopers.academy.course.model.Chapter>,
        lessons: List<com.asdevelopers.academy.course.model.Lesson>
    ): ValidationResult {
        // Branding خنثی فقط برای سازگاری API قدیمی است و در Bundle واقعی از Course می‌آید.
        val compatibilityBundle = CourseBundle(
            manifest = manifest,
            branding = com.asdevelopers.academy.course.model.CourseBranding("#6750A4", "#625B71", "#7D5260"),
            levels = levels,
            chapters = chapters,
            lessons = lessons
        )
        return validate(compatibilityBundle)
    }

    private fun validateManifest(bundle: CourseBundle, errors: MutableList<String>) {
        val manifest = bundle.manifest
        if (!stableIdPattern.matches(manifest.courseId)) errors += "courseId must use lowercase letters, digits and hyphens"
        if (manifest.titleFa.isBlank()) errors += "titleFa cannot be blank"
        if (manifest.titleEn.isBlank()) errors += "titleEn cannot be blank"
        if (SemanticVersion.parseOrNull(manifest.version) == null) errors += "course version is not valid SemVer"
        if (SemanticVersion.parseOrNull(manifest.curriculumVersion) == null) errors += "curriculumVersion is not valid SemVer"
        if (SemanticVersion.parseOrNull(manifest.minimumCoreVersion) == null) errors += "minimumCoreVersion is not valid SemVer"
        if (manifest.contentSchemaVersion <= 0) errors += "contentSchemaVersion must be positive"
        if (manifest.contentSchemaVersion > CoreVersion.COURSE_SCHEMA) {
            errors += "contentSchemaVersion ${manifest.contentSchemaVersion} requires a newer Core"
        }
        if (manifest.defaultLocale.isBlank()) errors += "defaultLocale cannot be blank"
        if (manifest.defaultLocale !in manifest.supportedLocales) errors += "supportedLocales must include defaultLocale"
        if (manifest.supportedLocales.any(String::isBlank)) errors += "supportedLocales cannot contain a blank locale"
        if (manifest.supportedLocales.distinct().size != manifest.supportedLocales.size) errors += "supportedLocales cannot contain duplicates"
        if (manifest.publisherId.isBlank()) errors += "publisherId cannot be blank"
        manifest.packageSha256?.takeIf { !sha256Pattern.matches(it) }?.let { errors += "packageSha256 must contain 64 hexadecimal characters" }
        listOf(bundle.branding.primaryColorHex, bundle.branding.secondaryColorHex, bundle.branding.accentColorHex)
            .filterNot(hexColorPattern::matches)
            .forEach { errors += "invalid branding color: $it" }
        val assetIds = bundle.assets.mapTo(mutableSetOf()) { it.id }
        listOfNotNull(bundle.branding.logoAssetId, bundle.branding.heroAssetId, bundle.branding.iconAssetId)
            .filter { it !in assetIds }
            .forEach { errors += "branding references missing asset $it" }
    }

    private fun validateQuizzes(
        bundle: CourseBundle,
        lessonIds: Set<String>,
        chapterIds: Set<String>,
        errors: MutableList<String>
    ) {
        bundle.quizzes.forEach { quiz ->
            if (quiz.courseId.isNotBlank() && quiz.courseId != bundle.manifest.courseId) errors += "quiz ${quiz.id} has a different courseId"
            quiz.lessonId?.takeIf { it !in lessonIds }?.let { errors += "quiz ${quiz.id} references missing lesson $it" }
            quiz.chapterId?.takeIf { it !in chapterIds }?.let { errors += "quiz ${quiz.id} references missing chapter $it" }
            if (quiz.lessonId == null && quiz.chapterId == null) errors += "quiz ${quiz.id} must reference a lesson or chapter"
            if (quiz.title.isBlank()) errors += "quiz ${quiz.id} has an empty title"
            if (quiz.questions.isEmpty()) errors += "quiz ${quiz.id} has no questions"
            checkIds("question in ${quiz.id}", quiz.questions.map { it.id }, errors)
            quiz.questions.forEach { question ->
                if (question.question.isBlank()) errors += "question ${question.id} has empty text"
                checkIds("answer in ${question.id}", question.answers.map { it.id }, errors)
                val correctCount = question.answers.count { it.isCorrect }
                if (question.type != QuestionType.FILL_CODE && question.answers.isEmpty()) errors += "question ${question.id} has no answers"
                when (question.type) {
                    QuestionType.MULTIPLE_SELECT -> if (correctCount < 1) {
                        errors += "question ${question.id} needs at least one correct answer"
                    }
                    QuestionType.FILL_CODE -> if (correctCount != 1) {
                        errors += "fill-code question ${question.id} needs exactly one correct text answer"
                    }
                    QuestionType.ORDER_STEPS -> {
                        val orders = question.answers.mapNotNull { it.order }
                        if (orders.size != question.answers.size || orders.distinct().size != orders.size) {
                            errors += "order-steps question ${question.id} needs a unique order for every answer"
                        }
                    }
                    QuestionType.MATCHING -> if (question.answers.any { it.matchKey.isNullOrBlank() }) {
                        errors += "matching question ${question.id} needs matchKey for every answer"
                    }
                    else -> if (correctCount != 1) {
                        errors += "question ${question.id} must have exactly one correct answer"
                    }
                }
            }
        }
    }

    private fun validateExercises(
        bundle: CourseBundle,
        lessonIds: Set<String>,
        errors: MutableList<String>
    ) {
        bundle.exercises.forEach { exercise ->
            if (exercise.courseId.isNotBlank() && exercise.courseId != bundle.manifest.courseId) errors += "exercise ${exercise.id} has a different courseId"
            exercise.lessonId?.takeIf { it !in lessonIds }?.let { errors += "exercise ${exercise.id} references missing lesson $it" }
            if (exercise.title.isBlank()) errors += "exercise ${exercise.id} has an empty title"
            if (exercise.description.isBlank()) errors += "exercise ${exercise.id} has an empty description"
            if (exercise.type in setOf(ExerciseType.COMPLETE_CODE, ExerciseType.WRITE_CODE, ExerciseType.FIX_CODE) && exercise.starterCode.isNullOrBlank()) {
                errors += "code exercise ${exercise.id} needs starterCode"
            }
        }
    }

    private fun validateProjects(
        bundle: CourseBundle,
        lessonIds: Set<String>,
        errors: MutableList<String>
    ) {
        bundle.projects.forEach { project ->
            if (project.courseId.isNotBlank() && project.courseId != bundle.manifest.courseId) errors += "project ${project.id} has a different courseId"
            if (project.title.isBlank()) errors += "project ${project.id} has an empty title"
            if (project.description.isBlank()) errors += "project ${project.id} has an empty description"
            if (project.estimatedMinutes <= 0) errors += "project ${project.id} estimatedMinutes must be positive"
            project.relatedLessonIds.filter { it !in lessonIds }.forEach {
                errors += "project ${project.id} references missing lesson $it"
            }
            checkIds("milestone in ${project.id}", project.milestones.map { it.id }, errors)
            if (project.milestones.isEmpty()) errors += "project ${project.id} has no milestones"
            if (project.milestones.any { it.title.isBlank() || it.description.isBlank() }) {
                errors += "project ${project.id} has an incomplete milestone"
            }
            checkUniqueOrder("milestone", mapOf(project.id to project.milestones), { it.order }, errors)
        }
    }

    private fun validateResources(
        bundle: CourseBundle,
        lessonIds: Set<String>,
        errors: MutableList<String>
    ) {
        bundle.glossary.forEach { entry ->
            if (entry.courseId.isNotBlank() && entry.courseId != bundle.manifest.courseId) errors += "glossary ${entry.id} has a different courseId"
            if (entry.term.isBlank()) errors += "glossary ${entry.id} has an empty term"
            if (entry.definition.isBlank()) errors += "glossary ${entry.id} has an empty definition"
            entry.relatedLessonIds.filter { it !in lessonIds }.forEach {
                errors += "glossary ${entry.id} references missing lesson $it"
            }
        }
        bundle.assets.forEach { asset ->
            if (asset.relativePath.isBlank()) errors += "asset ${asset.id} has an empty path"
            if (asset.mimeType.isBlank()) errors += "asset ${asset.id} has an empty mime type"
            asset.sha256?.takeIf { !sha256Pattern.matches(it) }?.let { errors += "asset ${asset.id} has invalid sha256" }
            asset.sizeBytes?.takeIf { it < 0 }?.let { errors += "asset ${asset.id} has negative size" }
        }
        bundle.references.forEach { reference ->
            if (reference.title.isBlank()) errors += "reference ${reference.id} has an empty title"
            val scheme = runCatching { java.net.URI(reference.url).scheme?.lowercase() }.getOrNull()
            if (scheme !in allowedSchemes) errors += "reference ${reference.id} must use http or https"
            reference.lessonId?.takeIf { it !in lessonIds }?.let { errors += "reference ${reference.id} references missing lesson $it" }
        }
    }

    /** Flashcard باید قابل جست‌وجو، قابل پیمایش و به درس واقعی همان Course متصل باشد. */
    private fun validateFlashcards(
        bundle: CourseBundle,
        lessonIds: Set<String>,
        errors: MutableList<String>
    ) {
        bundle.flashcards.forEach { flashcard ->
            if (flashcard.courseId != bundle.manifest.courseId) errors += "flashcard ${flashcard.id} has a different courseId"
            if (flashcard.lessonId !in lessonIds) errors += "flashcard ${flashcard.id} references missing lesson ${flashcard.lessonId}"
            if (flashcard.front.isBlank()) errors += "flashcard ${flashcard.id} has an empty front"
            if (flashcard.back.isBlank()) errors += "flashcard ${flashcard.id} has an empty back"
        }
    }

    private fun checkIds(kind: String, ids: List<String>, errors: MutableList<String>) {
        ids.filterNot(stableIdPattern::matches).forEach { errors += "$kind id '$it' is not stable" }
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { errors += "duplicate $kind id $it" }
    }

    private fun <T, K> checkUniqueOrder(
        kind: String,
        groups: Map<K, List<T>>,
        selector: (T) -> Int,
        errors: MutableList<String>
    ) {
        groups.forEach { (owner, values) ->
            values.groupingBy(selector).eachCount().filterValues { it > 1 }.keys.forEach {
                errors += "duplicate $kind order $it under $owner"
            }
        }
    }
}
