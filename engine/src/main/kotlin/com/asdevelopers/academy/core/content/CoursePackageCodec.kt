package com.asdevelopers.academy.core.content

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * تنها Codec رسمی Course Package؛ اپ‌های دوره‌ای نباید Json configuration جداگانه بسازند.
 */
class CoursePackageCodec(
    private val json: Json = defaultJson
) {
    /** متن JSON را به Bundle تایپ‌شده تبدیل می‌کند. */
    fun decode(rawJson: String): CourseBundle = json.decodeFromString(rawJson)

    /** Bundle را با قالب پایدار و قابل بازبینی برای Release محتوا می‌نویسد. */
    fun encode(bundle: CourseBundle): String = json.encodeToString(bundle)

    companion object {
        /** تنظیمات مشترک برای سازگاری رو به جلو و خروجی خوانا. */
        val defaultJson: Json = Json {
            // فیلد جدید Core نباید باعث شکست Package قدیمی یا جدیدتر شود.
            ignoreUnknownKeys = true
            // مقدارهای پیش‌فرض در Release نوشته می‌شوند تا قرارداد فایل شفاف بماند.
            encodeDefaults = true
            // nullهای غیرضروری حجم Package را افزایش نمی‌دهند.
            explicitNulls = false
            // خروجی Template و ابزار انتشار برای انسان قابل خواندن است.
            prettyPrint = true
        }
    }
}
