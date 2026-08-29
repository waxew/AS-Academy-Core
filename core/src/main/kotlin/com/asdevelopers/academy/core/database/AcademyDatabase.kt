package com.asdevelopers.academy.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * دیتابیس مرکزی داده‌های کاربر. محتوای Course قابل دانلود/بازسازی است و از داده شخصی جدا نگه داشته می‌شود.
 */
@Database(
    entities = [
        LessonProgressEntity::class,
        BookmarkEntity::class,
        QuizResultEntity::class,
        UserNoteEntity::class,
        SearchIndexEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AcademyDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun quizResultDao(): QuizResultDao
    abstract fun userNoteDao(): UserNoteDao
    abstract fun searchDao(): SearchDao

    companion object {
        /**
         * Factory به جای Singleton سراسری ارائه می‌شود تا اپ میزبان lifecycle و DI را کنترل کند.
         * destructive migration عمداً استفاده نمی‌شود تا داده کاربر پاک نشود.
         */
        fun create(context: Context, name: String = "as_academy.db"): AcademyDatabase =
            Room.databaseBuilder(context.applicationContext, AcademyDatabase::class.java, name)
                .build()
    }
}
