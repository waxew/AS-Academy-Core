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

include(":course")
include(":engine")
include(":tools")
include(":core")

// Optional provider implementation remains owned and versioned by Core while keeping the base
// Core artifact provider-neutral and usable by fully offline Academy applications.
include(":supabase-backend")

// Reference application رسمی Foundation در AS-Academy-MainUi/academy-viewer نگهداری و در cross-repo CI ساخته می‌شود.
