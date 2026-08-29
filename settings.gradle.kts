// فایل تنظیمات اصلی پروژه AS Academy Core.
// تمام ماژول‌های مشترک آکادمی از اینجا ثبت می‌شوند تا اپ‌های دوره‌ای مجبور به کپی کردن کدهای زیرساختی نباشند.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AS-Academy-Core"

// هسته عمومی قابل استفاده توسط تمام اپ‌های آموزشی.
include(":core")

// قرارداد و مدل Course Package؛ محتوای هیچ زبان خاصی در این ماژول قرار نمی‌گیرد.
include(":course")
