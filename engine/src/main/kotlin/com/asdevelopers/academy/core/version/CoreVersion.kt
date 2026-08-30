package com.asdevelopers.academy.core.version

/**
 * شماره‌های سازگاری عمومی در یک محل نگهداری می‌شوند تا Courseها مقدارهای پراکنده نسازند.
 */
object CoreVersion {
    /** نسخه API و Runtime این انتشار از AS Academy Core. */
    const val CURRENT: String = "1.0.1"

    /** جدیدترین نسخه Course JSON Schema که این Core می‌تواند مستقیماً بخواند. */
    const val COURSE_SCHEMA: Int = 1
}
