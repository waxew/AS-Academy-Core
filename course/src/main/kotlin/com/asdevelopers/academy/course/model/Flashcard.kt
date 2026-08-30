package com.asdevelopers.academy.course.model

import kotlinx.serialization.Serializable

/** سطح دشواری کارت برای فیلتر، مرور تطبیقی و گزارش پیشرفت. */
@Serializable
enum class FlashcardDifficulty {
    EASY,
    MEDIUM,
    HARD
}

/**
 * قرارداد عمومی یک Flashcard در AS Academy.
 *
 * خود Core رفتار نمایش، جست‌وجو و مرور را مدیریت می‌کند و Courseها فقط داده کارت را ارائه می‌دهند.
 * lessonId کارت را به یک درس واقعی متصل می‌کند تا مرور ضعیف‌ترین مباحث و Navigation قابل پیاده‌سازی باشد.
 */
@Serializable
data class Flashcard(
    /** شناسه پایدار کارت؛ پس از انتشار برای حفظ History مرور تغییر نمی‌کند. */
    val id: String,
    /** Course مالک کارت برای جلوگیری از مخلوط شدن داده دوره‌ها. */
    val courseId: String,
    /** درس مرتبط که باید در همان Course Package وجود داشته باشد. */
    val lessonId: String,
    /** متن روی کارت؛ معمولاً سؤال، اصطلاح یا Prompt کوتاه است. */
    val front: String,
    /** پاسخ یا توضیح پشت کارت. */
    val back: String,
    /** راهنمای اختیاری بدون افشای کامل پاسخ. */
    val hint: String? = null,
    /** برچسب‌ها برای Search، فیلتر و مرور موضوعی. */
    val tags: Set<String> = emptySet(),
    /** سختی پیش‌فرض کارت؛ موتور مرور می‌تواند در آینده آن را با عملکرد کاربر ترکیب کند. */
    val difficulty: FlashcardDifficulty = FlashcardDifficulty.MEDIUM
)
