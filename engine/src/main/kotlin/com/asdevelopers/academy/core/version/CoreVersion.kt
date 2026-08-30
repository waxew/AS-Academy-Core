package com.asdevelopers.academy.core.version

/**
 * شماره‌های سازگاری عمومی در یک محل نگهداری می‌شوند تا Courseها مقدارهای پراکنده نسازند.
 */
object CoreVersion {
    /** نسخه 1.3.0 مرکز مشترک و قابل جست‌وجوی Quiz/Exercise/Project را به API عمومی Core اضافه می‌کند. */
    const val CURRENT: String = "1.3.0"

    /** جدیدترین نسخه Course JSON Schema که این Core می‌تواند مستقیماً بخواند. */
    const val COURSE_SCHEMA: Int = 1
}
