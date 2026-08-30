package com.asdevelopers.academy.core.version

/**
 * شماره‌های سازگاری عمومی در یک محل نگهداری می‌شوند تا Courseها مقدارهای پراکنده نسازند.
 */
object CoreVersion {
    /** نسخه 1.2.0 wiring کامل Placement، Weak Review و Spaced Review را برای Hostهای Course اضافه می‌کند. */
    const val CURRENT: String = "1.2.0"

    /** جدیدترین نسخه Course JSON Schema که این Core می‌تواند مستقیماً بخواند. */
    const val COURSE_SCHEMA: Int = 1
}
