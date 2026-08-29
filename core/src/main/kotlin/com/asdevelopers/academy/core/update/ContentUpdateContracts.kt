package com.asdevelopers.academy.core.update

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** اطلاعات نسخه محتوایی قابل دانلود. */
data class ContentRelease(
    val courseId: String,
    val version: String,
    val minimumCoreVersion: String,
    val sha256: String,
    val downloadUrl: String
) {
    init {
        // Metadata ناقص قبل از دانلود رد می‌شود تا Provider رفتار مبهم نداشته باشد.
        require(courseId.isNotBlank()) { "courseId cannot be blank" }
        require(version.isNotBlank()) { "version cannot be blank" }
        require(minimumCoreVersion.isNotBlank()) { "minimumCoreVersion cannot be blank" }
        require(sha256.matches(Regex("^[A-Fa-f0-9]{64}$"))) { "sha256 must contain 64 hexadecimal characters" }
    }
}

/** Provider شبکه/فایل به Core تزریق می‌شود تا Core به سرویس خاصی وابسته نباشد. */
interface ContentUpdateProvider {
    suspend fun latest(courseId: String): ContentRelease?
    suspend fun download(release: ContentRelease, destination: File): File
}

/** نتیجه Check از نبود Release تا نتیجه دقیق نصب به UI منتقل می‌شود. */
sealed interface ContentUpdateCheckResult {
    data object NoRelease : ContentUpdateCheckResult
    data class Completed(val release: ContentRelease, val install: ContentInstallResult) : ContentUpdateCheckResult
    data class Failed(val reason: String, val cause: Throwable? = null) : ContentUpdateCheckResult
}

/**
 * Provider شبکه‌ای را به Installer فایل متصل می‌کند؛ Courseها فقط Provider سرویس خود را پیاده می‌کنند.
 */
class CourseContentUpdater(
    private val provider: ContentUpdateProvider,
    private val installer: FileCourseUpdateManager,
    private val downloadDirectory: File
) {
    suspend fun checkAndInstall(courseId: String, currentVersion: String?): ContentUpdateCheckResult {
        val release = try {
            provider.latest(courseId)
        } catch (error: Exception) {
            return ContentUpdateCheckResult.Failed(error.message ?: "Unable to check content update", error)
        } ?: return ContentUpdateCheckResult.NoRelease

        if (release.courseId != courseId) {
            return ContentUpdateCheckResult.Failed("Provider returned a release for ${release.courseId}, expected $courseId")
        }

        return try {
            val destination = withContext(Dispatchers.IO) {
                if (!downloadDirectory.exists() && !downloadDirectory.mkdirs()) {
                    error("Unable to create download directory")
                }
                File.createTempFile("academy-content-", ".download", downloadDirectory)
            }
            try {
                val downloaded = provider.download(release, destination)
                require(downloaded.canonicalFile == destination.canonicalFile) {
                    "ContentUpdateProvider must write to the supplied destination"
                }
                val bytes = withContext(Dispatchers.IO) { downloaded.readBytes() }
                ContentUpdateCheckResult.Completed(
                    release = release,
                    install = installer.install(bytes, release.sha256, currentVersion, expectedCourseId = courseId)
                )
            } finally {
                // فایل دانلود صرف‌نظر از نتیجه نصب پاک می‌شود؛ Installer نسخه فعال و Backup را مدیریت می‌کند.
                withContext(Dispatchers.IO) { destination.delete() }
            }
        } catch (error: Exception) {
            ContentUpdateCheckResult.Failed(error.message ?: "Unable to download content update", error)
        }
    }
}

/** Facade سازگاری API اولیه است و تمام منطق را به Sha256 مرکزی واگذار می‌کند. */
@Deprecated("Use Sha256 so hashing rules stay centralized")
object Sha256Verifier {
    fun hash(file: File): String = file.inputStream().use(Sha256::digest)

    fun matches(file: File, expected: String): Boolean = hash(file).equals(expected, ignoreCase = true)
}
