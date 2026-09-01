package com.asdevelopers.academy.core.content

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry فرایندی برای جایگزین کردن یک Asset با Package نصب‌شده و قبلاً Validateشده.
 *
 * Course Host همچنان می‌تواند از `AssetCoursePackageSource` موجود استفاده کند؛ Runtime Updater فقط
 * پس از Validate کامل فایل Update آن را برای Asset همان Course فعال می‌کند. Registry با بسته شدن
 * Process پاک می‌شود و در Launch بعدی باید دوباره از `CourseContentStore` فعال شود.
 */
object CoursePackageSourceOverrides {
    private val installedFiles = ConcurrentHashMap<String, File>()

    /** فایل نصب‌شده معتبر را برای Asset مشخص فعال می‌کند. */
    fun activate(assetPath: String, installedFile: File) {
        require(assetPath.isNotBlank()) { "assetPath cannot be blank" }
        require(installedFile.isFile) { "Installed course package does not exist: ${installedFile.absolutePath}" }
        installedFiles[assetPath] = installedFile
    }

    /** هنگام fallback یا quarantine، Asset داخلی دوباره منبع فعال می‌شود. */
    fun clear(assetPath: String) {
        installedFiles.remove(assetPath)
    }

    /** فقط Source داخلی Core مسیر Override را می‌خواند؛ Host نباید فایل را مستقیم مصرف کند. */
    internal fun resolve(assetPath: String): File? = installedFiles[assetPath]?.takeIf(File::isFile)
}
