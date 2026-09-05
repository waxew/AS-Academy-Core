package com.asdevelopers.academy.core.runtime

import android.content.Context
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
import com.asdevelopers.academy.core.settings.AcademyPreferencesRepository

/**
 * Composition root for the shared Academy runtime.
 *
 * Core owns database/repository/service wiring. UI shells and Course Apps consume this object
 * instead of rebuilding the persistence graph themselves. Keeping construction here guarantees
 * that schema/repository changes remain synchronized across MainUi and every Course host.
 */
class AcademyRuntime private constructor(
    val database: AcademyDatabase,
    val progressRepository: ProgressRepository,
    val bookmarkRepository: BookmarkRepository,
    val userNoteRepository: UserNoteRepository,
    val searchRepository: SearchRepository,
    val achievementRepository: AchievementRepository,
    val quizHistoryRepository: QuizHistoryRepository,
    val exerciseDraftRepository: ExerciseDraftRepository,
    val projectProgressRepository: ProjectProgressRepository,
    val learningCompletionRepository: LearningCompletionRepository,
    val preferencesRepository: AcademyPreferencesRepository,
    val studyReminderScheduler: StudyReminderScheduler
) {
    companion object {
        /** Creates the canonical offline-first runtime for an Academy host. */
        fun create(
            context: Context,
            databaseName: String = DEFAULT_DATABASE_NAME
        ): AcademyRuntime {
            val appContext = context.applicationContext
            val database = AcademyDatabase.create(appContext, databaseName)
            return fromDatabase(appContext, database)
        }

        /**
         * Builds a runtime around an existing database. This is useful for integration tests and
         * specialized hosts while preserving one canonical repository wiring implementation.
         */
        fun fromDatabase(
            context: Context,
            database: AcademyDatabase
        ): AcademyRuntime {
            val appContext = context.applicationContext
            return AcademyRuntime(
                database = database,
                progressRepository = ProgressRepository(database.progressDao()),
                bookmarkRepository = BookmarkRepository(database.bookmarkDao()),
                userNoteRepository = UserNoteRepository(database.userNoteDao()),
                searchRepository = SearchRepository(database.searchDao()),
                achievementRepository = AchievementRepository(database.achievementDao()),
                quizHistoryRepository = QuizHistoryRepository(database.quizResultDao()),
                exerciseDraftRepository = ExerciseDraftRepository(database.exerciseDraftDao()),
                projectProgressRepository = ProjectProgressRepository(database.projectProgressDao()),
                learningCompletionRepository = LearningCompletionRepository(database.learningCompletionDao()),
                preferencesRepository = AcademyPreferencesRepository(appContext),
                studyReminderScheduler = StudyReminderScheduler(appContext)
            )
        }

        const val DEFAULT_DATABASE_NAME: String = "as_academy.db"
    }
}
