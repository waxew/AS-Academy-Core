package com.asdevelopers.academy.core.database

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration این تست با SQLite واقعی Android اجرا می‌شود، نه mock.
 * هدف اصلی: داده‌های کاربر نسخه 3 حفظ شوند و جدول Review نسخه 4 بدون destructive migration اضافه شود.
 */
@RunWith(AndroidJUnit4::class)
class AcademyDatabaseMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "academy-migration-3-4-test.db"

    @Before
    fun cleanBefore() {
        // هر Test از فایل خالی شروع می‌شود تا نتیجه اجرای قبلی روی Migration اثر نگذارد.
        context.deleteDatabase(databaseName)
    }

    @After
    fun cleanAfter() {
        // فایل تست پس از اجرا حذف می‌شود تا storage دستگاه CI انباشته نشود.
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migration3To4PreservesUserDataAndCreatesReviewTable() = runBlocking {
        // ابتدا Schema دقیق نسخه جاری توسط خود Room ساخته می‌شود تا نام ستون‌ها/Indexها حدس زده نشوند.
        val current = AcademyDatabase.create(context, databaseName)
        current.progressDao().upsert(
            LessonProgressEntity(
                courseId = "basic",
                lessonId = "basic-lesson-migration",
                status = "IN_PROGRESS",
                progressPercent = 45,
                lastBlockIndex = 3,
                studySeconds = 420,
                lastOpenedAt = 1_000,
                completedAt = null
            )
        )
        current.userNoteDao().upsert(
            UserNoteEntity(
                id = "note-migration",
                courseId = "basic",
                lessonId = "basic-lesson-migration",
                blockId = "block-3",
                text = "این یادداشت باید بعد از Migration باقی بماند.",
                updatedAt = 1_100
            )
        )
        current.quizResultDao().insert(
            QuizResultEntity(
                attemptId = "attempt-migration",
                courseId = "basic",
                quizId = "basic-quiz-migration",
                scorePercent = 60,
                correctCount = 3,
                wrongCount = 2,
                weakTags = "loops|conditions",
                completedAt = 1_200
            )
        )
        current.close()

        // نسخه 4 فقط جدول Flashcard Progress را اضافه کرده است؛ حذف همان جدول فایل را به شکل v3 برمی‌گرداند.
        val databaseFile = context.getDatabasePath(databaseName)
        SQLiteDatabase.openDatabase(
            databaseFile.path,
            null,
            SQLiteDatabase.OPEN_READWRITE
        ).use { legacy ->
            legacy.execSQL("DROP INDEX IF EXISTS index_flashcard_progress_courseId_dueEpochDay")
            legacy.execSQL("DROP TABLE IF EXISTS flashcard_progress")
            // user_version=3 باعث می‌شود Room هنگام Open دقیقاً MIGRATION_3_4 را اجرا کند.
            legacy.version = 3
        }

        // Factory رسمی Core Migration را اجرا و سپس Schema نهایی را با Entityهای Room اعتبارسنجی می‌کند.
        val migrated = AcademyDatabase.create(context, databaseName)

        // داده‌های Progress، Note و Quiz که قابل بازسازی نیستند نباید تغییر یا حذف شوند.
        val progress = migrated.progressDao().getAll().single()
        assertEquals("basic", progress.courseId)
        assertEquals("basic-lesson-migration", progress.lessonId)
        assertEquals(45, progress.progressPercent)
        assertEquals(420L, progress.studySeconds)

        val note = migrated.userNoteDao().getAll().single()
        assertEquals("note-migration", note.id)
        assertEquals("این یادداشت باید بعد از Migration باقی بماند.", note.text)

        val quiz = migrated.quizResultDao().getAll().single()
        assertEquals("attempt-migration", quiz.attemptId)
        assertEquals("loops|conditions", quiz.weakTags)

        // DAO جدید باید بلافاصله قابل استفاده باشد؛ این بخش وجود جدول و Indexهای نسخه 4 را در عمل بررسی می‌کند.
        migrated.flashcardProgressDao().upsert(
            FlashcardProgressEntity(
                courseId = "basic",
                cardId = "flashcard-basic-glossary-loop",
                repetitions = 2,
                intervalDays = 3,
                easeFactor = 2.5,
                lastReviewedEpochDay = 20_000,
                dueEpochDay = 20_003,
                updatedAt = 1_300
            )
        )
        val reviewProgress = migrated.flashcardProgressDao().getAll()
        assertEquals(1, reviewProgress.size)
        assertTrue(reviewProgress.single().dueEpochDay > reviewProgress.single().lastReviewedEpochDay!!)

        migrated.close()
    }
}
