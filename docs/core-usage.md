# استفاده از AS Academy Core

## روش پیشنهادی در زمان توسعه: Composite Build

Repositoryهای Core و Course را مجاور هم clone کنید یا Core را به‌صورت Git submodule قرار دهید:

```text
workspace/
├── AS-Academy-Core/
└── AS-Academy-JavaScript/
```

در `settings.gradle.kts` پروژه JavaScript:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

includeBuild("../AS-Academy-Core")
include(":app")
```

در `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.asdevelopers.academy:core:1.0.0")
}
```

Gradle مختصات بالا را با پروژه `:core` در Included Build جایگزین می‌کند. مدل‌ها و Engineها Transitive هستند و اضافه‌کردن Dependency تکراری لازم نیست.

## آماده‌سازی Course Package

1. `course/template` را به پوشه‌ای مانند `course-content/` در Repository دوره کپی کنید.
2. `courseId`، Branding و تمام Stable IDها را تغییر دهید.
3. فایل‌های Lesson/Quiz/Exercise/Project را کامل کنید.
4. از خود Core، محتوا را Validate و Compile کنید:

```bash
../AS-Academy-Core/gradlew \
  -p ../AS-Academy-Core \
  :tools:run \
  --args="validate ../AS-Academy-JavaScript/course-content"

../AS-Academy-Core/gradlew \
  -p ../AS-Academy-Core \
  :tools:run \
  --args="compile ../AS-Academy-JavaScript/course-content ../AS-Academy-JavaScript/app/src/main/assets/course.json"
```

مسیرها نسبت به Root پروژه Core تفسیر می‌شوند؛ در CI بهتر است مسیر مطلق یا متغیر استاندارد Workspace استفاده شود.

## راه‌اندازی Runtime

```kotlin
val database = AcademyDatabase.create(applicationContext)
val preferences = AcademyPreferencesRepository(applicationContext)
val source = AssetCoursePackageSource(applicationContext, "course.json")
val result = CoursePackageLoader().load(source)
```

فقط `CourseLoadResult.Success` باید وارد UI و Importer شود. در موفقیت، برای ساخت Search Index:

```kotlin
CoursePackageImporter(database).import(result.bundle)
```

`import` علاوه بر بازسازی FTS، رکوردهای نسخه تک‌دوره‌ای را که `courseId` خالی دارند بر اساس Stable ID به همین Bundle متصل می‌کند؛ Course نباید Migration محلی دیگری بنویسد.

Host باید Repositoryها را به ViewModel یا State holder تزریق کند. اتصال مستقیم Composable به DAO یا فایل JSON مرز معماری را می‌شکند.

برای Home/Progress و دکمه «ادامه یادگیری»، محاسبه Levelها را در ViewModel دوباره پیاده نکنید:

```kotlin
val progressRepository = ProgressRepository(database.progressDao())
val dashboard = progressRepository.observeDashboard(result.bundle)
```

Flow بالا درصد کل، وضعیت بازبودن هر Level و `nextLessonId` را با همان قانون مشترک Core منتشر می‌کند. آستانه پیش‌فرض بازشدن Level بعدی ۸۰٪ است و فقط در صورت سیاست متفاوت محصول باید پارامتر آن تغییر کند.

## Shell و Navigation

از `AcademyTheme`، `AcademyAppShell` و `AcademyNavHost` استفاده کنید. گزینه‌های Course از `AcademyDrawerItem` تزریق می‌شوند و destinationهای خاص با `additionalGraph` اضافه می‌شوند. Settings، About و Route درس دوباره ساخته نمی‌شوند.

## قابلیت اختصاصی

برای Code Runner یک `CodeRunnerPlugin` در Repository دوره پیاده و در `CodeRunnerRegistry` ثبت کنید. برای تصویر/نمودار، Renderer اختصاصی را از Slot مربوط به `LessonRenderer` تزریق کنید. این Adapterها نباید Storage، Navigation یا Progress موازی بسازند.

## الزام README هر Course

بخش `AS Academy Core Usage` باید حداقل این موارد را ثبت کند:

- نسخه Core
- نسخه Course Content و Course Schema
- Capabilityهای فعال
- Adapterهای اختصاصی
- مسیر تولید `bundle.json`
- هر override موقت همراه Issue انتقال آن به Core
