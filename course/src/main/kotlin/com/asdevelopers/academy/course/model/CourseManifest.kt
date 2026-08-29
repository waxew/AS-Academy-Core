package com.asdevelopers.academy.course.model

/**
 * قرارداد اصلی هر Course Package در اکوسیستم AS Academy.
 *
 * این مدل در Core نگهداری می‌شود و هیچ دوره‌ای نباید نسخه دیگری از آن بسازد.
 * اپ‌های JavaScript، Python، Java و سایر دوره‌ها فقط داده متناسب با این قرارداد را فراهم می‌کنند.
 */
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
    val capabilities: CourseCapabilities
)

/**
 * Capabilityها اجازه می‌دهند یک Core واحد برای دوره‌های متفاوت استفاده شود.
 * برای مثال JavaScript به Code Runner نیاز دارد ولی Electronics بیشتر به Diagram Viewer متکی است.
 */
data class CourseCapabilities(
    val codeRunner: Boolean = false,
    val terminalExamples: Boolean = false,
    val diagrams: Boolean = false,
    val quizzes: Boolean = true,
    val exercises: Boolean = true,
    val projects: Boolean = true,
    val glossary: Boolean = true
)
