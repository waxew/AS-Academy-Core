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

// قرارداد محتوای دوره‌ها یک ماژول Kotlin خالص است تا ابزارهای تولید محتوا نیز بتوانند آن را مصرف کنند.
include(":course")

// موتورهای مستقل از Android در این ماژول قرار می‌گیرند و با Unit Test سریع بررسی می‌شوند.
include(":engine")

// ابزار خط فرمان به Course Repositoryها اجازه می‌دهد بدون اجرای Android، محتوا را Validate و Compile کنند.
include(":tools")

// Runtime اندروید شامل persistence، DataStore، WorkManager و platform infrastructure است.
include(":core")

// Reference application رسمی Foundation در AS-Academy-MainUi/academy-viewer نگهداری و در cross-repo CI ساخته می‌شود.
