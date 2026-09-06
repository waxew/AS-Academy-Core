package com.asdevelopers.academy.core.runtime

import android.content.Context
import com.asdevelopers.academy.core.backend.AcademyBackend
import com.asdevelopers.academy.core.backend.OfflineAcademyBackend
import com.asdevelopers.academy.core.database.AcademyDatabase
import com.asdevelopers.academy.core.notification.StudyReminderScheduler
import com.asdevelopers.academy.core.repository.AchievementRepository
import com.asdevelopers.academy.core.repository.BookmarkRepository
import com.asdevelopers.academy.core.repository.ExerciseDraftRepository
import com.asdevelopers.academy.core.repository.LearningCompletionRepository
import com.asdevelopers.academy.core.repository.ProgressRepository
import com.asdevelopers.academy.core.repository.ProjectProgressRepository
import com.asdevelopers.academy.core.repository.QuizHistoryRepository
import com.asdevelopers.academy.core.repository.SearchRepository
import com.asdevelopers.academy.core.repository.UserNoteRepository
import com.asdevelopers.academy.core.repository.UserSyncOutboxRepository
import com.asdevelopers.academy.core.settings.AcademyPreferencesRepository

/**
 * ریشه ساخت و اتصال اجزای اصلی Core در تمام برنامه‌های AS Academy.
 *
 * این کلاس Composition Root معماری Core است؛ یعنی تمام وابستگی‌های اصلی
 * مانند Database، Repository ها، Backend و Scheduler در این نقطه ساخته می‌شوند.
 *
 * مسئولیت‌ها:
 * - ایجاد نمونه Database مرکزی
 * - ساخت Repository های مورد استفاده برنامه‌ها
 * - اتصال Backend به Core
 * - آماده‌سازی سرویس‌های مشترک آموزشی
 *
 * قوانین معماری:
 * - MainUi نباید Database یا Repository بسازد.
 * - MainCourse نباید جزئیات Core را مدیریت کند.
 * - App های آموزشی فقط Runtime را مصرف می‌کنند.
 *
 * جریان وابستگی:
 *
 * Application
 *      |
 *      v
 * AcademyRuntime
 *      |
 *      +-- Database
 *      +-- Repository Layer
 *      +-- Backend
 *      +-- Notification Services
 *
 * این کلاس نباید شامل منطق نمایش UI یا منطق اختصاصی یک Course باشد.
 */
class AcademyRuntime private constructor(
    internal val database: AcademyDatabase,
    val backend: AcademyBackend,
    val progressRepository: ProgressRepository,
    val bookmarkRepository: BookmarkRepository,
    val userNoteRepository: UserNoteRepository,
    val searchRepository: SearchRepository,
    val achievementRepository: AchievementRepository,
    val quizHistoryRepository: QuizHistoryRepository,
    val exerciseDraftRepository: ExerciseDraftRepository,
    val projectProgressRepository: ProjectProgressRepository,
    val learningCompletionRepository: LearningCompletionRepository,
    val userSyncOutboxRepository: UserSyncOutboxRepository,
    val preferencesRepository: AcademyPreferencesRepository,
    val studyReminderScheduler: StudyReminderScheduler
) {
    companion object {
        @JvmStatic
        /**
         * ایجاد Runtime اصلی برنامه.
         *
         * مراحل اجرا:
         * 1- دریافت Application Context
         * 2- ساخت Database
         * 3- ایجاد Repository ها
         * 4- اتصال Backend
         * 5- آماده‌سازی سرویس‌های Core
         *
         * خروجی:
         * یک Runtime کامل که توسط App ها مصرف می‌شود.
         */
        fun create(
            context: Context,
            databaseName: String = "as_academy.db",
            backend: AcademyBackend = OfflineAcademyBackend
        ): AcademyRuntime {
            val appContext = context.applicationContext
            val database = AcademyDatabase.create(appContext, databaseName)
            return AcademyRuntime(
                database = database,
                backend = backend,
                progressRepository = ProgressRepository(database.progressDao()),
                bookmarkRepository = BookmarkRepository(database.bookmarkDao()),
                userNoteRepository = UserNoteRepository(database.userNoteDao()),
                searchRepository = SearchRepository(database.searchDao()),
                achievementRepository = AchievementRepository(database.achievementDao()),
                quizHistoryRepository = QuizHistoryRepository(database.quizResultDao()),
                exerciseDraftRepository = ExerciseDraftRepository(database.exerciseDraftDao()),
                projectProgressRepository = ProjectProgressRepository(database.projectProgressDao()),
                learningCompletionRepository = LearningCompletionRepository(database.learningCompletionDao()),
                userSyncOutboxRepository = UserSyncOutboxRepository(database.syncOutboxDao(), backend.userSync),
                preferencesRepository = AcademyPreferencesRepository(appContext),
                studyReminderScheduler = StudyReminderScheduler(appContext)
            )
        }
    }
}
