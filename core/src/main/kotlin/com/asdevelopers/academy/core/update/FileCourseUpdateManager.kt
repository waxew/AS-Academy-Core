package com.asdevelopers.academy.core.update

import com.asdevelopers.academy.core.content.CourseLoadResult
import com.asdevelopers.academy.core.content.CoursePackageLoader
import com.asdevelopers.academy.core.content.FileCoursePackageSource
import java.io.File

/** نتیجه نصب فایل Content با علت دقیق برای UI و Log. */
sealed interface ContentInstallResult {
    data class Installed(val version: String, val warnings: List<String>) : ContentInstallResult
    data class Rejected(val reason: String, val errors: List<String> = emptyList()) : ContentInstallResult
    data class Failed(val reason: String, val cause: Throwable? = null) : ContentInstallResult
}

/**
 * نصب Atomic فایل Course: ابتدا Hash و Contract بررسی، سپس نسخه قبلی Backup و در خطا Rollback می‌شود.
 */
class FileCourseUpdateManager(
    private val installDirectory: File,
    private val coreVersion: String,
    private val loader: CoursePackageLoader = CoursePackageLoader()
) {
    /**
     * تصمیم Metadata-level قبل از دانلود Package.
     * همان Planner در مرحله نصب نیز دوباره روی Manifest واقعی Package اجرا می‌شود تا Metadata قابل اعتماد فرض نشود.
     */
    fun plan(
        currentVersion: String?,
        candidateVersion: String,
        minimumCoreVersion: String
    ): UpdateDecision = CourseUpdatePlanner.decide(
        currentVersion = currentVersion,
        candidateVersion = candidateVersion,
        minimumCoreVersion = minimumCoreVersion,
        coreVersion = coreVersion
    )

    suspend fun install(
        candidateBytes: ByteArray,
        expectedSha256: String,
        currentVersion: String?,
        expectedCourseId: String? = null
    ): ContentInstallResult {
        // Hash قبل از Decode کنترل می‌شود تا فایل ناقص یا دستکاری‌شده پردازش نشود.
        if (!Sha256.matches(candidateBytes, expectedSha256)) {
            return ContentInstallResult.Rejected("SHA-256 verification failed")
        }

        // پوشه اختصاصی برنامه نیاز به مجوز Storage عمومی ندارد.
        if (!installDirectory.exists() && !installDirectory.mkdirs()) {
            return ContentInstallResult.Failed("Unable to create content directory")
        }
        val temporary = File(installDirectory, "course-package.tmp")
        val installed = File(installDirectory, "course-package.json")
        val backup = File(installDirectory, "course-package.backup")

        return try {
            // فایل موقت هر بار کامل بازنویسی می‌شود و هیچ Package نیمه‌دانلودی نصب نمی‌شود.
            temporary.writeBytes(candidateBytes)
            when (val load = loader.load(FileCoursePackageSource(temporary))) {
                is CourseLoadResult.Invalid -> ContentInstallResult.Rejected("Course validation failed", load.errors)
                is CourseLoadResult.Failure -> ContentInstallResult.Failed(load.message, load.cause)
                is CourseLoadResult.Success -> {
                    // Provider اجازه ندارد Package یک Course دیگر را در مسیر نصب فعلی جایگزین کند.
                    if (expectedCourseId != null && load.bundle.manifest.courseId != expectedCourseId) {
                        return ContentInstallResult.Rejected(
                            "Course ID mismatch: expected $expectedCourseId, received ${load.bundle.manifest.courseId}"
                        )
                    }
                    // Metadata preflight فقط بهینه‌سازی است؛ تصمیم روی Manifest واقعی Package دوباره بررسی می‌شود.
                    val decision = plan(
                        currentVersion = currentVersion,
                        candidateVersion = load.bundle.manifest.version,
                        minimumCoreVersion = load.bundle.manifest.minimumCoreVersion
                    )
                    if (decision != UpdateDecision.INSTALL) {
                        return ContentInstallResult.Rejected("Update decision: $decision")
                    }
                    // نسخه نصب‌شده تا پایان جایگزینی نگه داشته می‌شود تا Rollback امکان‌پذیر باشد.
                    if (backup.exists()) backup.delete()
                    if (installed.exists() && !installed.renameTo(backup)) {
                        return ContentInstallResult.Failed("Unable to create rollback backup")
                    }
                    if (!temporary.renameTo(installed)) {
                        // شکست جایگزینی باعث بازگرداندن نسخه سالم قبلی می‌شود.
                        if (backup.exists()) backup.renameTo(installed)
                        return ContentInstallResult.Failed("Unable to activate course package")
                    }
                    ContentInstallResult.Installed(load.bundle.manifest.version, load.warnings)
                }
            }
        } catch (error: Exception) {
            // در هر خطای I/O نسخه فعال قبلی دست‌نخورده یا بازیابی می‌شود.
            if (!installed.exists() && backup.exists()) backup.renameTo(installed)
            ContentInstallResult.Failed(error.message ?: "Content install failed", error)
        } finally {
            // فایل موقت پس از موفقیت یا شکست باقی نمی‌ماند.
            temporary.takeIf(File::exists)?.delete()
        }
    }

    /** آخرین نسخه Backupشده را برای Rollback دستی فعال می‌کند. */
    fun rollback(): Boolean {
        val installed = File(installDirectory, "course-package.json")
        val backup = File(installDirectory, "course-package.backup")
        if (!backup.exists()) return false
        val failed = File(installDirectory, "course-package.failed")
        failed.delete()
        if (installed.exists() && !installed.renameTo(failed)) return false
        val restored = backup.renameTo(installed)
        if (!restored && failed.exists()) failed.renameTo(installed)
        if (restored) failed.delete()
        return restored
    }
}
