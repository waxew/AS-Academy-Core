package com.asdevelopers.academy.core.version

/**
 * شماره‌های سازگاری عمومی در یک محل نگهداری می‌شوند تا Courseها مقدارهای پراکنده نسازند.
 */
object CoreVersion {
    /** نسخه 1.4.0 کانال Runtime Content Update مستقل از APK و preflight قبل از دانلود را فراهم می‌کند. */
    const val CURRENT: String = "1.4.0"

    /** جدیدترین نسخه Course JSON Schema که این Core می‌تواند مستقیماً بخواند. */
    const val COURSE_SCHEMA: Int = 1
}
