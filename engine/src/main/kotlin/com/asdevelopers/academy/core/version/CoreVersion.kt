package com.asdevelopers.academy.core.version

/**
 * شماره‌های سازگاری عمومی در یک محل نگهداری می‌شوند تا Courseها مقدارهای پراکنده نسازند.
 */
object CoreVersion {
    /**
     * نسخه API و Runtime این انتشار از AS Academy Core.
     * Flashcard contract/review/persistence یک قابلیت عمومی backward-compatible است، بنابراین Minor افزایش یافته است.
     */
    const val CURRENT: String = "1.1.0"

    /** جدیدترین نسخه Course JSON Schema که این Core می‌تواند مستقیماً بخواند. */
    const val COURSE_SCHEMA: Int = 1
}
