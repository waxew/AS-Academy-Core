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
import com.asdevelopers.academy.core.settings.AcademyPreferencesRepository

/**
 * The single composition root for every Academy application.
 *
 * Course applications and MainUi consume this runtime; they never create Core databases, DAOs,
 * repositories, schedulers or backend SDK clients themselves. Implementation details stay owned
 * by Core so platform changes propagate to every Academy application from one place.
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
    val preferencesRepository: AcademyPreferencesRepository,
    val studyReminderScheduler: StudyReminderScheduler
) {
    companion object {
        @JvmStatic
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
                preferencesRepository = AcademyPreferencesRepository(appContext),
                studyReminderScheduler = StudyReminderScheduler(appContext)
            )
        }
    }
}
