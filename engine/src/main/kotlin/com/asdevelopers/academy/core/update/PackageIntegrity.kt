package com.asdevelopers.academy.core.update

import com.asdevelopers.academy.core.version.SemanticVersion
import java.io.InputStream
import java.security.MessageDigest

/** نتیجه تصمیم موتور Update بدون وابستگی به شبکه یا Android. */
enum class UpdateDecision {
    INSTALL,
    ALREADY_CURRENT,
    DOWNGRADE_BLOCKED,
    CORE_UPDATE_REQUIRED,
    INVALID_VERSION
}

/** ابزار مرکزی SHA-256 برای Package، Backup و فایل‌های انتشار. */
object Sha256 {
    /** Hash بایت‌ها را با حروف کوچک و طول ثابت 64 کاراکتر تولید می‌کند. */
    fun digest(bytes: ByteArray): String = digest(bytes.inputStream())

    /** Stream فایل بزرگ بدون نگهداری کل محتوا در حافظه Hash می‌شود. */
    fun digest(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    /** مقایسه بدون حساسیت به حروف برای Hashهای واردشده از Server انجام می‌شود. */
    fun matches(bytes: ByteArray, expected: String): Boolean =
        digest(bytes).equals(expected.trim(), ignoreCase = true)
}

/** قواعد نسخه قبل از نصب Content Update در یک مکان اعمال می‌شوند. */
object CourseUpdatePlanner {
    fun decide(currentVersion: String?, candidateVersion: String, minimumCoreVersion: String, coreVersion: String): UpdateDecision {
        val candidate = SemanticVersion.parseOrNull(candidateVersion) ?: return UpdateDecision.INVALID_VERSION
        val requiredCore = SemanticVersion.parseOrNull(minimumCoreVersion) ?: return UpdateDecision.INVALID_VERSION
        val installedCore = SemanticVersion.parseOrNull(coreVersion) ?: return UpdateDecision.INVALID_VERSION
        if (installedCore < requiredCore) return UpdateDecision.CORE_UPDATE_REQUIRED
        // نبود نسخه به معنی نصب نخست است؛ نسخه موجودِ نامعتبر به‌صورت جداگانه خطا محسوب می‌شود.
        if (currentVersion == null) return UpdateDecision.INSTALL
        val current = SemanticVersion.parseOrNull(currentVersion) ?: return UpdateDecision.INVALID_VERSION
        return when {
            candidate > current -> UpdateDecision.INSTALL
            candidate == current -> UpdateDecision.ALREADY_CURRENT
            else -> UpdateDecision.DOWNGRADE_BLOCKED
        }
    }
}
