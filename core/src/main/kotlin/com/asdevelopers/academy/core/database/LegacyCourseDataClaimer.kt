package com.asdevelopers.academy.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import com.asdevelopers.academy.core.content.CourseBundle
import com.asdevelopers.academy.core.progress.LearningTargetType

/**
 * داده نسخه تک‌دوره‌ای را با کمک Stable IDهای Bundle به Course واقعی متصل می‌کند.
 *
 * این عملیات داخل Transaction مربوط به [com.asdevelopers.academy.core.content.CoursePackageImporter]
 * اجرا می‌شود. `UPDATE OR IGNORE` داده جدیدتر را حفظ می‌کند و حذف دوم فقط رکورد قدیمیِ ادغام‌شده را پاک می‌کند.
 */
internal object LegacyCourseDataClaimer {
    fun claim(database: SupportSQLiteDatabase, bundle: CourseBundle) {
        val courseId = bundle.manifest.courseId
        val lessonIds = bundle.lessons.map { it.id }
        val quizIds = bundle.quizzes.map { it.id }
        val exerciseIds = bundle.exercises.map { it.id }
        val projectIds = bundle.projects.map { it.id }

        // Progress و داده‌های دارای کلید ترکیبی ممکن است با رکورد جدید برخورد کنند؛ در آن حالت رکورد جدید حفظ می‌شود.
        lessonIds.forEach { lessonId ->
            claimCompositeRecord(database, "lesson_progress", "lessonId", lessonId, courseId)
        }
        exerciseIds.forEach { exerciseId ->
            claimCompositeRecord(database, "exercise_drafts", "exerciseId", exerciseId, courseId)
            claimCompletion(database, courseId, LearningTargetType.EXERCISE, exerciseId)
        }
        projectIds.forEach { projectId ->
            claimCompositeRecord(database, "project_progress", "projectId", projectId, courseId)
            claimCompletion(database, courseId, LearningTargetType.PROJECT, projectId)
        }

        // Primary key این جدول‌ها مستقل از Course است و تغییر courseId باعث برخورد کلید نمی‌شود.
        quizIds.forEach { quizId ->
            claimSimpleRecord(database, "quiz_results", "quizId", quizId, courseId)
        }
        lessonIds.forEach { lessonId ->
            claimSimpleRecord(database, "user_notes", "lessonId", lessonId, courseId)
        }

        val bookmarkTargetIds = buildSet {
            addAll(lessonIds)
            bundle.lessons.forEach { lesson -> addAll(lesson.blocks.map { it.id }) }
            addAll(quizIds)
            addAll(exerciseIds)
            addAll(projectIds)
            addAll(bundle.glossary.map { it.id })
            addAll(bundle.assets.map { it.id })
            addAll(bundle.references.map { it.id })
        }
        bookmarkTargetIds.forEach { targetId ->
            claimSimpleRecord(database, "bookmarks", "targetId", targetId, courseId)
        }

        // Search داده کاربر نیست؛ ایندکس بدون Course از نسخه قدیمی حذف و پایین‌دست همین Transaction بازسازی می‌شود.
        database.execSQL("DELETE FROM search_index WHERE courseId = ''")
    }

    private fun claimSimpleRecord(
        database: SupportSQLiteDatabase,
        table: String,
        idColumn: String,
        id: String,
        courseId: String
    ) {
        database.execSQL(
            "UPDATE `$table` SET courseId = ? WHERE courseId = '' AND `$idColumn` = ?",
            arrayOf(courseId, id)
        )
    }

    private fun claimCompositeRecord(
        database: SupportSQLiteDatabase,
        table: String,
        idColumn: String,
        id: String,
        courseId: String
    ) {
        database.execSQL(
            "UPDATE OR IGNORE `$table` SET courseId = ? WHERE courseId = '' AND `$idColumn` = ?",
            arrayOf(courseId, id)
        )
        database.execSQL(
            "DELETE FROM `$table` WHERE courseId = '' AND `$idColumn` = ?",
            arrayOf(id)
        )
    }

    private fun claimCompletion(
        database: SupportSQLiteDatabase,
        courseId: String,
        targetType: LearningTargetType,
        targetId: String
    ) {
        val stableKey = "$courseId:${targetType.name}:$targetId"
        database.execSQL(
            "UPDATE OR IGNORE learning_completion SET courseId = ?, `key` = ? " +
                "WHERE courseId = '' AND targetType = ? AND targetId = ?",
            arrayOf(courseId, stableKey, targetType.name, targetId)
        )
        database.execSQL(
            "DELETE FROM learning_completion WHERE courseId = '' AND targetType = ? AND targetId = ?",
            arrayOf(targetType.name, targetId)
        )
    }
}
