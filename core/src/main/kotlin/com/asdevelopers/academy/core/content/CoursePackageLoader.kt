package com.asdevelopers.academy.core.content

import android.content.Context
import androidx.room.withTransaction
import com.asdevelopers.academy.core.database.AcademyDatabase
import com.asdevelopers.academy.core.database.LegacyCourseDataClaimer
import com.asdevelopers.academy.core.repository.FlashcardReviewRepository
import com.asdevelopers.academy.core.repository.SearchRepository
import com.asdevelopers.academy.core.search.SearchDocumentFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** منبع Course Package می‌تواند Asset داخلی یا فایل Downloadشده باشد. */
fun interface CoursePackageSource {
    suspend fun readText(): String
}

/** Asset داخلی نسخه اولیه Offline دوره را بدون نیاز به اینترنت فراهم می‌کند. */
class AssetCoursePackageSource(
    private val context: Context,
    private val assetPath: String
) : CoursePackageSource {
    override suspend fun readText(): String = withContext(Dispatchers.IO) {
        // Asset بزرگ روی Main thread خوانده نمی‌شود تا اولین Frame اپ مسدود نشود.
        context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}

/** فایل نصب‌شده توسط Content Updater با همان Codec رسمی خوانده می‌شود. */
class FileCoursePackageSource(private val file: File) : CoursePackageSource {
    override suspend fun readText(): String = withContext(Dispatchers.IO) {
        // فایل Update نیز ممکن است چند مگابایت باشد و همیشه روی Dispatcher.IO خوانده می‌شود.
        file.readText(Charsets.UTF_8)
    }
}

/** نتیجه Load خطاهای Contract را از Exceptionهای مبهم جدا می‌کند. */
sealed interface CourseLoadResult {
    data class Success(val bundle: CourseBundle, val warnings: List<String>) : CourseLoadResult
    data class Invalid(val errors: List<String>, val warnings: List<String>) : CourseLoadResult
    data class Failure(val message: String, val cause: Throwable? = null) : CourseLoadResult
}

/**
 * Loader واحد Course، JSON را Decode و قبل از ورود به UI/Database اعتبارسنجی می‌کند.
 */
class CoursePackageLoader(
    private val codec: CoursePackageCodec = CoursePackageCodec(),
    private val validator: CoursePackageValidator = CoursePackageValidator()
) {
    suspend fun load(source: CoursePackageSource): CourseLoadResult = try {
        val bundle = codec.decode(source.readText())
        val validation = validator.validate(bundle)
        if (validation.isValid) {
            CourseLoadResult.Success(bundle, validation.warnings)
        } else {
            CourseLoadResult.Invalid(validation.errors, validation.warnings)
        }
    } catch (error: Exception) {
        CourseLoadResult.Failure(error.message ?: "Unable to read course package", error)
    }
}

/** Importer تمام داده‌های مشتق‌شده و State اولیه قابل‌بازسازی Course را در Transaction واحد آماده می‌کند. */
class CoursePackageImporter(
    private val database: AcademyDatabase,
    private val searchRepository: SearchRepository = SearchRepository(database.searchDao()),
    private val flashcardReviewRepository: FlashcardReviewRepository = FlashcardReviewRepository(database.flashcardReviewDao())
) {
    suspend fun import(bundle: CourseBundle) {
        // اتصال داده Legacy، Seed کارت‌های تازه و ساخت Search Index با هم Commit می‌شوند تا Import نیمه‌کاره دیده نشود.
        database.withTransaction {
            LegacyCourseDataClaimer.claim(database.openHelper.writableDatabase, bundle)
            flashcardReviewRepository.seedCourse(
                courseId = bundle.manifest.courseId,
                cards = bundle.flashcards
            )
            searchRepository.replaceCourse(
                courseId = bundle.manifest.courseId,
                documents = SearchDocumentFactory.from(bundle)
            )
        }
    }
}
