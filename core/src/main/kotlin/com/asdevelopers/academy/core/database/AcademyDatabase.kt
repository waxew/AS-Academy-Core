package com.asdevelopers.academy.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * دیتابیس مرکزی داده‌های مهم کاربر.
 *
 * Course Package و متن Search قابل بازسازی‌اند و در DB ذخیره نمی‌شوند؛ Progress، Note، Draft،
 * Completion، Review Progress و Sync Outbox در Migration حفظ می‌شوند.
 */
@Database(
    entities = [
        LessonProgressEntity::class,
        LearningCompletionEntity::class,
        BookmarkEntity::class,
        QuizResultEntity::class,
        UserNoteEntity::class,
        SearchIndexEntity::class,
        ExerciseDraftEntity::class,
        ProjectProgressEntity::class,
        AchievementEntity::class,
        FlashcardProgressEntity::class,
        SyncOutboxEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AcademyDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    abstract fun learningCompletionDao(): LearningCompletionDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun quizResultDao(): QuizResultDao
    abstract fun userNoteDao(): UserNoteDao
    abstract fun searchDao(): SearchDao
    abstract fun exerciseDraftDao(): ExerciseDraftDao
    abstract fun projectProgressDao(): ProjectProgressDao
    abstract fun achievementDao(): AchievementDao
    abstract fun flashcardProgressDao(): FlashcardProgressDao
    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {
        /**
         * Factory به جای Singleton سراسری ارائه می‌شود تا Host چرخه عمر و Dependency Injection را کنترل کند.
         * هیچ destructive fallback تعریف نشده است؛ نبود Migration باید Build/Test را متوقف کند، نه داده کاربر را پاک کند.
         */
        fun create(context: Context, name: String = "as_academy.db"): AcademyDatabase =
            Room.databaseBuilder(context.applicationContext, AcademyDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()

        /** نسخه اولیه تک‌دوره‌ای را به ساختار چنددوره‌ای و Repositoryهای کامل ارتقا می‌دهد. */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                migrateLegacyTablesToCourseAwareSchema(database)
            }
        }

        /**
         * نسخه 2 در دو شاخه آزمایشی منتشر شد: یکی فقط Completion داشت و دیگری ساختار چنددوره‌ای.
         * Migration با بررسی واقعی ستون‌ها هر دو شکل را بدون حذف داده به Schema واحد نسخه 3 می‌رساند.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                if (!database.hasColumn("lesson_progress", "courseId")) {
                    migrateLegacyTablesToCourseAwareSchema(database)
                }
                ensureLearningCompletionTable(database)
            }
        }

        /**
         * نسخه 4 فقط جدول مستقل Flashcard Progress را اضافه می‌کند؛ هیچ جدول یا ستون قبلی بازنویسی نمی‌شود.
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                ensureFlashcardProgressTable(database)
            }
        }

        /**
         * نسخه 5 متن مشتق‌شده Course را از FTS پاک می‌کند و فقط Outbox کوچک داده‌های کاربر را اضافه می‌کند.
         * جدول legacy FTS برای سازگاری Room خالی می‌ماند؛ Core v5 دیگر هیچ محتوای Course در آن insert نمی‌کند.
         */
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DELETE FROM search_index")
                ensureSyncOutboxTable(database)
            }
        }

        private fun migrateLegacyTablesToCourseAwareSchema(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS lesson_progress_new (
                    courseId TEXT NOT NULL,
                    lessonId TEXT NOT NULL,
                    status TEXT NOT NULL,
                    progressPercent INTEGER NOT NULL,
                    lastBlockIndex INTEGER NOT NULL,
                    studySeconds INTEGER NOT NULL,
                    lastOpenedAt INTEGER,
                    completedAt INTEGER,
                    PRIMARY KEY(courseId, lessonId)
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO lesson_progress_new (
                    courseId, lessonId, status, progressPercent, lastBlockIndex, studySeconds, lastOpenedAt, completedAt
                )
                SELECT '', lessonId,
                    CASE WHEN completed = 1 THEN 'COMPLETED' WHEN progressPercent > 0 THEN 'IN_PROGRESS' ELSE 'NOT_STARTED' END,
                    progressPercent, lastBlockIndex, studySeconds, lastOpenedAt,
                    CASE WHEN completed = 1 THEN lastOpenedAt ELSE NULL END
                FROM lesson_progress
                """.trimIndent()
            )
            database.execSQL("DROP TABLE lesson_progress")
            database.execSQL("ALTER TABLE lesson_progress_new RENAME TO lesson_progress")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_lesson_progress_lessonId ON lesson_progress (lessonId)")

            if (!database.hasColumn("bookmarks", "courseId")) {
                database.execSQL("ALTER TABLE bookmarks ADD COLUMN courseId TEXT NOT NULL DEFAULT ''")
            }
            database.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_courseId ON bookmarks (courseId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_courseId_targetId ON bookmarks (courseId, targetId)")

            if (!database.hasColumn("quiz_results", "courseId")) {
                database.execSQL("ALTER TABLE quiz_results ADD COLUMN courseId TEXT NOT NULL DEFAULT ''")
            }
            if (!database.hasColumn("quiz_results", "weakTags")) {
                database.execSQL("ALTER TABLE quiz_results ADD COLUMN weakTags TEXT NOT NULL DEFAULT ''")
            }
            database.execSQL("CREATE INDEX IF NOT EXISTS index_quiz_results_courseId_quizId ON quiz_results (courseId, quizId)")

            if (!database.hasColumn("user_notes", "courseId")) {
                database.execSQL("ALTER TABLE user_notes ADD COLUMN courseId TEXT NOT NULL DEFAULT ''")
            }
            database.execSQL("CREATE INDEX IF NOT EXISTS index_user_notes_courseId_lessonId ON user_notes (courseId, lessonId)")

            database.execSQL("DROP TABLE IF EXISTS search_index")
            database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS search_index USING FTS4(courseId, refId, refType, title, body)")

            database.execSQL(
                "CREATE TABLE IF NOT EXISTS exercise_drafts (courseId TEXT NOT NULL, exerciseId TEXT NOT NULL, answer TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(courseId, exerciseId))"
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS project_progress (courseId TEXT NOT NULL, projectId TEXT NOT NULL, completedMilestoneIds TEXT NOT NULL, draft TEXT NOT NULL, updatedAt INTEGER NOT NULL, completedAt INTEGER, PRIMARY KEY(courseId, projectId))"
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS achievements (courseId TEXT NOT NULL, achievementId TEXT NOT NULL, unlockedAt INTEGER NOT NULL, PRIMARY KEY(courseId, achievementId))"
            )
        }

        private fun ensureLearningCompletionTable(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS learning_completion (`key` TEXT NOT NULL, courseId TEXT NOT NULL DEFAULT '', targetType TEXT NOT NULL, targetId TEXT NOT NULL, completed INTEGER NOT NULL, completedAt INTEGER NOT NULL, PRIMARY KEY(`key`))"
            )
            if (!database.hasColumn("learning_completion", "courseId")) {
                database.execSQL("ALTER TABLE learning_completion ADD COLUMN courseId TEXT NOT NULL DEFAULT ''")
            }
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_learning_completion_courseId_targetType ON learning_completion (courseId, targetType)"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_learning_completion_courseId_targetId ON learning_completion (courseId, targetId)"
            )
        }

        private fun ensureFlashcardProgressTable(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS flashcard_progress (
                    courseId TEXT NOT NULL,
                    cardId TEXT NOT NULL,
                    repetitions INTEGER NOT NULL,
                    intervalDays INTEGER NOT NULL,
                    easeFactor REAL NOT NULL,
                    lastReviewedEpochDay INTEGER,
                    dueEpochDay INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(courseId, cardId)
                )
                """.trimIndent()
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_flashcard_progress_courseId_dueEpochDay ON flashcard_progress (courseId, dueEpochDay)"
            )
        }

        private fun ensureSyncOutboxTable(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS sync_outbox (
                    operationId TEXT NOT NULL,
                    courseId TEXT NOT NULL,
                    entityType TEXT NOT NULL,
                    entityId TEXT NOT NULL,
                    payloadJson TEXT NOT NULL,
                    updatedAtIso8601 TEXT NOT NULL,
                    deletedAtIso8601 TEXT,
                    createdAtEpochMillis INTEGER NOT NULL,
                    attemptCount INTEGER NOT NULL,
                    nextAttemptAtEpochMillis INTEGER NOT NULL,
                    PRIMARY KEY(operationId)
                )
                """.trimIndent()
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_sync_outbox_courseId_nextAttemptAtEpochMillis ON sync_outbox (courseId, nextAttemptAtEpochMillis)"
            )
        }

        /** PRAGMA به‌جای فرض نسخه، Schema واقعی نصب کاربر را بررسی می‌کند. */
        private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean =
            query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                var found = false
                while (cursor.moveToNext() && !found) {
                    found = nameIndex >= 0 && cursor.getString(nameIndex) == column
                }
                found
            }
    }
}
