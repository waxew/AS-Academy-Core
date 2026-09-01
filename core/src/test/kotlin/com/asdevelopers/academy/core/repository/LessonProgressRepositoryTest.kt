package com.asdevelopers.academy.core.repository

import com.asdevelopers.academy.core.database.LessonProgressEntity
import com.asdevelopers.academy.core.database.ProgressDao
import com.asdevelopers.academy.core.progress.LessonStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/** Lesson Progress باید monotonic و update-safe باشد. */
class LessonProgressRepositoryTest {
    @Test
    fun `reading position never moves progress backwards`() = runBlocking {
        val dao = FakeProgressDao()
        val repository = LessonProgressRepository(dao)

        repository.markOpened("basic", "lesson-1", 100)
        repository.savePosition("basic", "lesson-1", lastBlockIndex = 7, totalBlocks = 10, studySecondsDelta = 60, updatedAtEpochMillis = 200)
        val updated = repository.savePosition("basic", "lesson-1", lastBlockIndex = 2, totalBlocks = 10, studySecondsDelta = 30, updatedAtEpochMillis = 300)

        assertEquals(LessonStatus.IN_PROGRESS, updated.status)
        assertEquals(80, updated.progressPercent)
        assertEquals(7, updated.lastBlockIndex)
        assertEquals(90, updated.studySeconds)
    }

    @Test
    fun `completed lesson stays completed when reopened`() = runBlocking {
        val dao = FakeProgressDao()
        val repository = LessonProgressRepository(dao)

        repository.markCompleted("basic", "lesson-1", completedAtEpochMillis = 500, lastBlockIndex = 9)
        val reopened = repository.markOpened("basic", "lesson-1", openedAtEpochMillis = 800)

        assertEquals(LessonStatus.COMPLETED, reopened.status)
        assertEquals(100, reopened.progressPercent)
        assertEquals(500, reopened.completedAtEpochMillis)
        assertEquals(800, reopened.lastOpenedAtEpochMillis)
    }

    @Test
    fun `mark needs review preserves completion evidence`() = runBlocking {
        val dao = FakeProgressDao()
        val repository = LessonProgressRepository(dao)

        repository.markCompleted("basic", "lesson-1", completedAtEpochMillis = 500)
        val review = repository.markNeedsReview("basic", "lesson-1", updatedAtEpochMillis = 900)

        assertEquals(LessonStatus.NEEDS_REVIEW, review.status)
        assertEquals(100, review.progressPercent)
        assertEquals(500, review.completedAtEpochMillis)
    }
}

/** Fake کوچک فقط قرارداد ProgressDao را برای تست Repository اجرا می‌کند. */
private class FakeProgressDao : ProgressDao {
    private val state = MutableStateFlow<List<LessonProgressEntity>>(emptyList())

    override fun observe(courseId: String, lessonId: String): Flow<LessonProgressEntity?> =
        kotlinx.coroutines.flow.map(state) { items -> items.firstOrNull { it.courseId == courseId && it.lessonId == lessonId } }

    override fun observeCourse(courseId: String): Flow<List<LessonProgressEntity>> =
        kotlinx.coroutines.flow.map(state) { items -> items.filter { it.courseId == courseId } }

    override fun observeCourseWithLegacy(courseId: String): Flow<List<LessonProgressEntity>> =
        kotlinx.coroutines.flow.map(state) { items -> items.filter { it.courseId == courseId || it.courseId.isBlank() } }

    override suspend fun getAll(): List<LessonProgressEntity> = state.value

    override suspend fun upsert(entity: LessonProgressEntity) {
        state.value = state.value.filterNot {
            it.courseId == entity.courseId && it.lessonId == entity.lessonId
        } + entity
    }

    override suspend fun upsertAll(items: List<LessonProgressEntity>) {
        items.forEach { upsert(it) }
    }
}
