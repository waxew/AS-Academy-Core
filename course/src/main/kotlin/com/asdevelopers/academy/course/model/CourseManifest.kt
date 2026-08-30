package com.asdevelopers.academy.course.model

import kotlinx.serialization.Serializable

/**
 * قرارداد اصلی هر Course Package در اکوسیستم AS Academy.
 *
 * این مدل در Core نگهداری می‌شود و هیچ دوره‌ای نباید نسخه دیگری از آن بسازد.
 * اپ‌های JavaScript، Python، Java و سایر دوره‌ها فقط داده متناسب با این قرارداد را فراهم می‌کنند.
 */
@Serializable
data class CourseManifest(
    /** شناسه پایدار و یکتای دوره؛ پس از انتشار نباید تغییر کند. */
    val courseId: String,
    /** نام فارسی قابل نمایش دوره. */
    val titleFa: String,
    /** نام انگلیسی قابل نمایش دوره. */
    val titleEn: String,
    /** نسخه مستقل محتوای دوره. */
    val version: String,
    /** نسخه Schema محتوایی که Package با آن ساخته شده است. */
    val contentSchemaVersion: Int,
    /** حداقل نسخه Core مورد نیاز برای خواندن صحیح این Package. */
    val minimumCoreVersion: String,
    /** تعیین می‌کند محتوای اصلی دوره راست‌به‌چپ است یا خیر. */
    val rtl: Boolean,
    /** قابلیت‌های اختصاصی که Core باید برای این دوره فعال کند. */
    val capabilities: CourseCapabilities,
    /** زبان پیش‌فرض رابط و محتوای دوره؛ مقدار استاندارد دوره‌های فارسی `fa` است. */
    val defaultLocale: String = "fa",
    /** زبان‌هایی که همین Course Package به‌طور کامل پشتیبانی می‌کند. */
    val supportedLocales: List<String> = listOf(defaultLocale),
    /** نسخه برنامه آموزشی برای تشخیص تغییر ساختار سرفصل‌ها مستقل از نسخه فایل‌هاست. */
    val curriculumVersion: String = version,
    /** شناسه ناشر برای جلوگیری از جایگزینی Package رسمی با محتوای ناشناس استفاده می‌شود. */
    val publisherId: String = "as-team",
    /** SHA-256 اختیاری کل Package در زمان انتشار بیرون از خود Package ثبت می‌شود. */
    val packageSha256: String? = null
)

/**
 * Capabilityها اجازه می‌دهند یک Core واحد برای دوره‌های متفاوت استفاده شود.
 * برای مثال JavaScript به Code Runner نیاز دارد ولی Electronics بیشتر به Diagram Viewer متکی است.
 */
@Serializable
data class CourseCapabilities(
    val codeRunner: Boolean = false,
    val terminalExamples: Boolean = false,
    val diagrams: Boolean = false,
    val quizzes: Boolean = true,
    val exercises: Boolean = true,
    val projects: Boolean = true,
    val glossary: Boolean = true,
    /** Flashcard یک قابلیت مشترک Core است؛ Course فقط کارت‌های اختصاصی خود را می‌آورد. */
    val flashcards: Boolean = true,
    val bookmarks: Boolean = true,
    val userNotes: Boolean = true,
    val achievements: Boolean = true,
    val offlineContent: Boolean = true
)
