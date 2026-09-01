package com.asdevelopers.academy.core.update

import android.content.Context
import com.asdevelopers.academy.core.content.AssetCoursePackageSource
import com.asdevelopers.academy.core.content.CourseLoadResult
import com.asdevelopers.academy.core.content.CoursePackageLoader
import com.asdevelopers.academy.core.content.CoursePackageSourceOverrides
import com.asdevelopers.academy.core.content.FileCoursePackageSource
import com.asdevelopers.academy.core.version.CoreVersion
import com.asdevelopers.academy.core.version.SemanticVersion
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
 * نسخه نصب‌شده و Asset داخل APK هر دو Validate می‌شوند و جدیدترین نسخه معتبر محلی انتخاب می‌شود.
 * این قاعده مانع می‌شود یک Runtime Update قدیمی بعد از نصب APK جدیدتر روی Course جدید Bundleشده سایه بیندازد.
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
     * جدیدترین منبع معتبر محلی را Resolve می‌کند.
     *
     * - Asset داخل APK همیشه بدون Override خوانده و Validate می‌شود.
     * - Package نصب‌شده نیز مستقل Validate و با courseId مورد انتظار تطبیق داده می‌شود.
     * - فقط وقتی نسخه نصب‌شده از Asset واقعاً جدیدتر باشد، Installed Update انتخاب می‌شود.
     * - نسخه نصب‌شده مساوی/قدیمی از مسیر فعال خارج می‌شود تا APK جدیدتر در حالت Offline عقب نرود.
     */
    suspend fun loadPreferred(): CourseContentSnapshot {
        // مقایسه باید Asset واقعی APK را ببیند، نه Override باقی‌مانده از Compose قبلی همین Process.
        CoursePackageSourceOverrides.clear(bundledAssetPath)
        val bundled = normalizeCourseId(
            loader.load(AssetCoursePackageSource(appContext, bundledAssetPath)),
            sourceLabel = "Bundled"
        )
        val bundledSuccess = bundled as? CourseLoadResult.Success

        if (!installedPackageFile.isFile) {
            return CourseContentSnapshot(bundled, CourseContentOrigin.BUNDLED_ASSET)
        }

        val installedRaw = loader.load(FileCoursePackageSource(installedPackageFile))
        val installed = normalizeCourseId(installedRaw, sourceLabel = "Installed")
        val installedSuccess = installed as? CourseLoadResult.Success

        if (installedSuccess == null) {
            val reason = when (installed) {
                is CourseLoadResult.Invalid -> "invalid"
                is CourseLoadResult.Failure -> "failed"
                is CourseLoadResult.Success -> "unexpected"
            }
            quarantineInstalledPackage(reason)
            return CourseContentSnapshot(
                result = bundled.withWarning(
                    "Installed content was quarantined and the bundled offline course was selected"
                ),
                origin = CourseContentOrigin.BUNDLED_ASSET
            )
        }

        // اگر APK asset خراب باشد اما Runtime Package معتبر قبلی وجود داشته باشد، آموزش از دسترس خارج نمی‌شود.
        if (bundledSuccess == null) {
            return CourseContentSnapshot(installedSuccess, CourseContentOrigin.INSTALLED_UPDATE)
        }

        val installedVersion = SemanticVersion.parseOrNull(installedSuccess.bundle.manifest.version)
        val bundledVersion = SemanticVersion.parseOrNull(bundledSuccess.bundle.manifest.version)

        // Validator باید SemVer را تضمین کند؛ این شاخه دفاعی مانع انتخاب Package مبهم در صورت Contract Regression است.
        if (installedVersion == null) {
            quarantineInstalledPackage("invalid-version")
            return CourseContentSnapshot(
                bundledSuccess.withWarning("Installed content had an invalid semantic version and was ignored"),
                CourseContentOrigin.BUNDLED_ASSET
            )
        }
        if (bundledVersion == null) {
            return CourseContentSnapshot(
                installedSuccess.withWarning("Bundled content had an invalid semantic version; installed content was retained"),
                CourseContentOrigin.INSTALLED_UPDATE
            )
        }

        return if (installedVersion > bundledVersion) {
            CourseContentSnapshot(installedSuccess, CourseContentOrigin.INSTALLED_UPDATE)
        } else {
            // Equal version نیز Bundle را برنده می‌کند؛ تغییر محتوا بدون version bump طبق قرارداد MainCourse مجاز نیست.
            quarantineInstalledPackage("superseded")
            CourseContentSnapshot(
                bundledSuccess.withWarning(
                    "Bundled content ${bundledSuccess.bundle.manifest.version} superseded installed content ${installedSuccess.bundle.manifest.version}"
                ),
                CourseContentOrigin.BUNDLED_ASSET
            )
        }
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

    /** Package خراب/قدیمی از نام فعال خارج می‌شود؛ Backup Installer مستقل باقی می‌ماند. */
    private suspend fun quarantineInstalledPackage(reason: String) = withContext(Dispatchers.IO) {
        CoursePackageSourceOverrides.clear(bundledAssetPath)
        if (!installedPackageFile.exists()) return@withContext
        if (!installDirectory.exists() && !installDirectory.mkdirs()) return@withContext
        val rejected = File(installDirectory, "course-package.$reason.rejected")
        rejected.delete()
        installedPackageFile.renameTo(rejected)
    }

    /** موفقیت Course با شناسه اشتباه به Invalid صریح تبدیل می‌شود تا هر دو منبع یک Policy داشته باشند. */
    private fun normalizeCourseId(result: CourseLoadResult, sourceLabel: String): CourseLoadResult =
        if (result is CourseLoadResult.Success && result.bundle.manifest.courseId != courseId) {
            CourseLoadResult.Invalid(
                errors = listOf("$sourceLabel courseId ${result.bundle.manifest.courseId} does not match expected $courseId"),
                warnings = result.warnings
            )
        } else {
            result
        }

    /** Warning فقط به Result موفق افزوده می‌شود؛ خطاهای اصلی Invalid/Failure دست‌نخورده می‌مانند. */
    private fun CourseLoadResult.withWarning(message: String): CourseLoadResult =
        if (this is CourseLoadResult.Success) copy(warnings = listOf(message) + warnings) else this
}
