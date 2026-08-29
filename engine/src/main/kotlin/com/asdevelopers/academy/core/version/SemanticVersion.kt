package com.asdevelopers.academy.core.version

/**
 * نمایش استاندارد SemVer برای مقایسه نسخه Core، Database و Course Package.
 * بخش pre-release از نسخه پایدار پایین‌تر در نظر گرفته می‌شود.
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null
) : Comparable<SemanticVersion> {

    init {
        // هیچ بخش عددی نسخه نمی‌تواند منفی باشد.
        require(major >= 0 && minor >= 0 && patch >= 0) { "Semantic version numbers cannot be negative" }
    }

    override fun compareTo(other: SemanticVersion): Int {
        // ابتدا سه بخش عددی طبق استاندارد SemVer مقایسه می‌شوند.
        compareValues(major, other.major).takeIf { it != 0 }?.let { return it }
        compareValues(minor, other.minor).takeIf { it != 0 }?.let { return it }
        compareValues(patch, other.patch).takeIf { it != 0 }?.let { return it }

        // نسخه بدون pre-release از نسخه آزمایشی همان شماره جدیدتر است.
        if (preRelease == null && other.preRelease != null) return 1
        if (preRelease != null && other.preRelease == null) return -1
        if (preRelease == null) return 0

        // شناسه‌های pre-release جداگانه مقایسه می‌شوند؛ مثلاً beta.10 از beta.2 جدیدتر است.
        val leftIdentifiers = preRelease.split('.')
        val rightIdentifiers = requireNotNull(other.preRelease).split('.')
        leftIdentifiers.zip(rightIdentifiers).forEach { (left, right) ->
            comparePreReleaseIdentifier(left, right).takeIf { it != 0 }?.let { return it }
        }
        // اگر همه بخش‌های مشترک برابر باشند، نسخه‌ای که بخش اضافه دارد جدیدتر است.
        return compareValues(leftIdentifiers.size, rightIdentifiers.size)
    }

    override fun toString(): String = buildString {
        // سه بخش عددی همیشه نوشته می‌شوند تا نسخه مبهم نباشد.
        append("$major.$minor.$patch")
        // شناسه آزمایشی فقط در صورت وجود به نسخه افزوده می‌شود.
        preRelease?.let { append("-$it") }
    }

    companion object {
        // Regex فقط قالب SemVer اصلی و pre-release را می‌پذیرد؛ build metadata روی سازگاری اثر ندارد.
        private val pattern = Regex("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-([0-9A-Za-z.-]+))?(?:\\+([0-9A-Za-z.-]+))?$")

        /** رشته نسخه را می‌خواند یا در صورت نامعتبر بودن null برمی‌گرداند. */
        fun parseOrNull(value: String): SemanticVersion? {
            // Match کامل مانع پذیرفتن نسخه‌های ناقص مانند 1.0 می‌شود.
            val match = pattern.matchEntire(value.trim()) ?: return null
            val preRelease = match.groupValues[4].ifBlank { null }
            val buildMetadata = match.groupValues[5].ifBlank { null }
            // نقطه خالی و صفر ابتدایی در شناسه عددی pre-release طبق SemVer معتبر نیستند.
            if (!isValidIdentifiers(preRelease, forbidNumericLeadingZero = true)) return null
            if (!isValidIdentifiers(buildMetadata, forbidNumericLeadingZero = false)) return null
            return SemanticVersion(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groupValues[2].toIntOrNull() ?: return null,
                patch = match.groupValues[3].toIntOrNull() ?: return null,
                preRelease = preRelease
            )
        }

        private fun isValidIdentifiers(value: String?, forbidNumericLeadingZero: Boolean): Boolean {
            if (value == null) return true
            return value.split('.').all { identifier ->
                identifier.isNotEmpty() &&
                    identifier.all { it.isLetterOrDigit() || it == '-' } &&
                    !(forbidNumericLeadingZero && identifier.all(Char::isDigit) && identifier.length > 1 && identifier.startsWith('0'))
            }
        }

        private fun comparePreReleaseIdentifier(left: String, right: String): Int {
            val leftIsNumber = left.all(Char::isDigit)
            val rightIsNumber = right.all(Char::isDigit)
            return when {
                leftIsNumber && rightIsNumber ->
                    compareValues(left.length, right.length).takeIf { it != 0 } ?: left.compareTo(right)
                leftIsNumber -> -1
                rightIsNumber -> 1
                else -> left.compareTo(right)
            }
        }

        /** همان parse است ولی برای ورودی داخلی نامعتبر خطای واضح تولید می‌کند. */
        fun parse(value: String): SemanticVersion =
            requireNotNull(parseOrNull(value)) { "Invalid semantic version: $value" }
    }
}
