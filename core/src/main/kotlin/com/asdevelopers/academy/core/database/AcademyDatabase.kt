package com.asdevelopers.academy.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** دیتابیس مرکزی داده‌های کاربر؛ محتوای Course از داده شخصی جدا است. */
@Database(
    entities = [
        LessonProgressEntity::class,
        LearningCompletionEntity::class,
        BookmarkEntity::class,
        QuizResultEntity::class,
        UserNoteEntity::class,
        SearchIndexEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AcademyDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    abstract fun learningCompletionDao(): LearningCompletionDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun quizResultDao(): QuizResultDao
    abstract fun userNoteDao(): UserNoteDao
    abstract fun searchDao(): SearchDao

    companion object {
        /** Migration غیرتخریبی برای حفظ پیشرفت کاربران نسخه قبلی. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `learning_completion` (`key` TEXT NOT NULL, `targetType` TEXT NOT NULL, `targetId` TEXT NOT NULL, `completed` INTEGER NOT NULL, `completedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))"
                )
            }
        }

        fun create(context: Context, name: String = "as_academy.db"): AcademyDatabase =
            Room.databaseBuilder(context.applicationContext, AcademyDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
