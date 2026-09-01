package com.asdevelopers.academy.core.update

import android.content.Context
import com.asdevelopers.academy.core.content.AssetCoursePackageSource
import com.asdevelopers.academy.core.content.CourseLoadResult
import com.asdevelopers.academy.core.content.CoursePackageLoader
import com.asdevelopers.academy.core.content.CoursePackageSourceOverrides
import com.asdevelopers.academy.core.content.FileCoursePackageSource
import com.asdevelopers.academy.core.version.CoreVersion
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** مشخص می‌کند Course فعال از Update مستقل آمده یا از Asset امن داخل APK. */
enum class CourseContentOrigin {
    INSTALLED_UPDATE,
    BUNDLED_ASSET
}

/** نتیجه Resolve علاوه بر خود Course، منبع فعال را برای Log/QA نگه می‌دارد. */
data class CourseContentSnapshot(
    val result: CourseLoadResult,
    val origin: CourseContentOrigin
)

/**
 * Store مشترک Content برای همه Course Appها.
 *
 * اول Package نصب‌شده و Validateشده را امتحان می‌کند؛ در نبود/خرابی آن بدون از دست رفتن قابلیت آفلاین
 * به Asset داخلی APK برمی‌گردد. فایل خراب قرنطینه می‌شود تا هر Launch دوباره همان خطا را تکرار نکند.
 */
class CourseContentStore(
    context: Context,
    private val courseId: String,
    private val bundledAssetPath: String,
    private val loader: CoursePackageLoader = CoursePackageLoader(),
    rootDirectory: File = File(context.filesDir, "academy-content")
) {
    private val appContext = context.applicationContext

    init {
        // Course ID بخشی از مسیر فایل است؛ ورودی باید Stable ID ساده باشد و traversal نداشته باشد.
        require(courseId.matches(Regex("^[A-Za-z0-9._-]+$")) && !courseId.contains("..")) {
            "courseId contains unsupported path characters"
        }
        require(bundledAssetPath.isNotBlank()) { "bundledAssetPath cannot be blank" }
    }

    /** پوشه خصوصی نصب Update همین Course؛ Installer و Resolver دقیقاً از یک مسیر استفاده می‌کنند. */
    val installDirectory: File = File(rootDirectory, courseId)

    /** فایل فعال نصب‌شده توسط FileCourseUpdateManager. */
    val installedPackageFile: File
        get() = File(installDirectory, "course-package.json")

    /** Installer استاندارد با نسخه واقعی Core ساخته می‌شود تا minimumCoreVersion اعمال شود. */
    fun updateManager(coreVersion: String = CoreVersion.CURRENT): FileCourseUpdateManager =
        FileCourseUpdateManager(
            installDirectory = installDirectory,
            coreVersion = coreVersion,
            loader = loader
        )

    /**
     * منبع ترجیحی را Load می‌کند. Update فقط زمانی فعال است که Contract معتبر و courseId دقیقاً درست باشد.
     */
    suspend fun loadPreferred(): CourseContentSnapshot {
        if (installedPackageFile.isFile) {
            when (val installed = loader.load(FileCoursePackageSource(installedPackageFile))) {
                is CourseLoadResult.Success -> {
                    if (installed.bundle.manifest.courseId == courseId) {
                        return CourseContentSnapshot(installed, CourseContentOrigin.INSTALLED_UPDATE)
                    }
                    quarantineInstalledPackage("course-id-mismatch")
                    return loadBundled(
                        "Installed content was ignored because its courseId did not match $courseId"
                    )
                }
                is CourseLoadResult.Invalid -> {
                    quarantineInstalledPackage("invalid")
                    return loadBundled(
                        "Installed content failed validation and the bundled offline course was restored"
                    )
                }
                is CourseLoadResult.Failure -> {
                    quarantineInstalledPackage("failed")
                    return loadBundled(
                        "Installed content could not be read and the bundled offline course was restored"
                    )
                }
            }
        }
        return loadBundled()
    }

    /**
     * Snapshot معتبر را برای Loaderهای موجود Host فعال می‌کند.
     * Hostهای فعلی که `AssetCoursePackageSource` دارند بدون بازنویسی Screen/Navigation همان Package را می‌خوانند.
     */
    fun activate(snapshot: CourseContentSnapshot): CourseContentSnapshot {
        when {
            snapshot.origin == CourseContentOrigin.INSTALLED_UPDATE &&
                snapshot.result is CourseLoadResult.Success &&
                installedPackageFile.isFile -> CoursePackageSourceOverrides.activate(
                    bundledAssetPath,
                    installedPackageFile
                )
            else -> CoursePackageSourceOverrides.clear(bundledAssetPath)
        }
        return snapshot
    }

    /** Resolve و Activate را در یک فراخوانی انجام می‌دهد تا Host ترتیب این دو مرحله را تکرار نکند. */
    suspend fun loadAndActivatePreferred(): CourseContentSnapshot = activate(loadPreferred())

    /** Asset همیشه آخرین شبکه ایمنی است و خود آن نیز توسط Loader رسمی Validate می‌شود. */
    private suspend fun loadBundled(extraWarning: String? = null): CourseContentSnapshot {
        // قبل از خواندن fallback، Override قبلی پاک می‌شود تا AssetCoursePackageSource وارد حلقه نشود.
        CoursePackageSourceOverrides.clear(bundledAssetPath)
        val loaded = loader.load(AssetCoursePackageSource(appContext, bundledAssetPath))
        val result = if (extraWarning != null && loaded is CourseLoadResult.Success) {
            loaded.copy(warnings = listOf(extraWarning) + loaded.warnings)
        } else {
            loaded
        }
        return CourseContentSnapshot(result, CourseContentOrigin.BUNDLED_ASSET)
    }

    /** Package خراب از نام فعال خارج می‌شود؛ Backup نصب‌شده توسط Installer مستقل باقی می‌ماند. */
    private suspend fun quarantineInstalledPackage(reason: String) = withContext(Dispatchers.IO) {
        CoursePackageSourceOverrides.clear(bundledAssetPath)
        if (!installedPackageFile.exists()) return@withContext
        if (!installDirectory.exists() && !installDirectory.mkdirs()) return@withContext
        val rejected = File(installDirectory, "course-package.$reason.rejected")
        rejected.delete()
        installedPackageFile.renameTo(rejected)
    }
}
