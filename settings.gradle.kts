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

// Runtime اندروید شامل Room، DataStore، WorkManager و اجزای Compose است.
include(":core")

// این برنامه مرجع، نحوه اتصال یک Course Repository به Core را به‌صورت اجرایی نشان می‌دهد.
include(":sample-app")
