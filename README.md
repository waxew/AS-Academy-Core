# AS Academy Core

اسکلت مرکزی و قابل اجرای همه اپ‌های آموزشی AS Academy؛ این Repository محل واحد کدهای مشترک است تا پروژه‌های JavaScript، Python، Java و دوره‌های بعدی Navigation، Database، UI و موتورهای آموزشی را دوباره ننویسند.

نسخه فعلی فقط یک تعریف معماری یا پوشه خالی نیست: قرارداد محتوای تایپ‌شده، موتورهای JVM، Runtime اندروید، ابزار Validate/Compile، Course Template، Sample App، تست و CI در خود Repository قرار دارند.

## قانون اصلی

اگر یک قابلیت در بیش از یک دوره کاربرد دارد، باید اینجا پیاده‌سازی شود. Repository هر دوره فقط این موارد را نگه می‌دارد:

- Branding و تنظیم Capabilityها
- Adapter واقعاً اختصاصی، مانند JavaScript Code Runner
- تنظیمات نهایی اپ، Package Name و Signing خارج از Core
- آدرس کانال محتوای همان Course

محتوای اصلی دوره‌ها در `AS-Academy-MainCourse` نگهداری می‌شود و Course App نباید نسخه مستقل و قابل ویرایش از محتوای اصلی را Fork کند.

کپی‌کردن Navigation، Room، Progress، Quiz، Search، Bookmark، Settings، Drawer/Profile، Lesson Renderer، Update، Backup، Placement، Review Engine یا Learning Catalog داخل Course Repository مجاز نیست.

## نسخه فعلی

- Core/API: **1.4.0**
- Course schema: **1**
- Room database schema: **4**
- Backup schema: **3**

## ماژول‌های واقعی

| ماژول | نوع | مسئولیت |
|---|---|---|
| `course` | Kotlin/JVM | قرارداد Serializable برای Manifest، محتوا، Branding و منابع |
| `engine` | Kotlin/JVM | Validator، Codec، Progress، Quiz، Exercise، Project، Search، Review، Placement، Achievement، Code Runner API، Version و Update rules |
| `core` | Android Library | Room، Repository، DataStore، WorkManager، Navigation Compose، Theme، Drawer، Renderer، Adaptive Review UI، Learning Catalog و Runtime Content Update |
| `tools` | JVM CLI | اعتبارسنجی پوشه Course و ساخت `bundle.json` بدون Android SDK |
| `sample-app` | Android App | نمونه اجرایی اتصال Course Package به Core و APIهای مشترک |

جریان وابستگی یک‌طرفه است:

```text
course <- engine <- core <- course application
             ^          ^
             |          |
           tools     sample-app
```

## شروع سریع

نیازمندی‌ها: JDK 17، Android SDK 36 و Gradle Wrapper موجود در Repository.

```bash
./gradlew :course:test :engine:test
./gradlew :tools:run --args="validate course/template"
./gradlew :tools:run --args="compile course/template build/course-bundle.json"
./gradlew :core:lintDebug :sample-app:assembleDebug
```

برای شروع یک دوره، ساختار Course Package باید در `AS-Academy-MainCourse/courses/<course-id>/course` ایجاد و با CLI بالا Validate شود. Course App فقط خروجی Compileشده را در Build داخل APK قرار می‌دهد و در Runtime می‌تواند نسخه محتوایی جدیدتر را از کانال رسمی MainCourse دریافت کند.

## اتصال از Repository دوره

Core را به‌صورت Git submodule یا checkout مجاور نگه دارید و در `settings.gradle.kts` پروژه دوره معرفی کنید:

```kotlin
includeBuild("../AS-Academy-Core")
```

سپس تنها وابستگی Runtime را به اپ اضافه کنید؛ `engine` و `course` به‌صورت Transitive می‌آیند:

```kotlin
dependencies {
    implementation("com.asdevelopers.academy:core:1.4.0")
}
```

جزئیات کامل در [راهنمای مصرف Core](docs/core-usage.md) و [راهنمای اتصال یک دوره](docs/integration-guide.md) آمده است.

## Runtime Content Update مستقل از APK

از Core 1.4.0، محتوای آموزشی می‌تواند بدون انتشار نسخه جدید APK به‌روزرسانی شود. این قابلیت جایگزین Asset داخلی نمی‌شود؛ Asset داخل APK همچنان نسخه امن و آفلاین دوره است.

جریان استاندارد:

```text
AS-Academy-MainCourse
        |
        | Validate + Compile
        v
latest.json + course-package.json
        |
        | HTTPS metadata
        v
HttpsJsonContentUpdateProvider
        |
        v
CourseContentUpdater
        |
        | SemVer + minimumCoreVersion preflight
        | download only if installable/newer
        v
FileCourseUpdateManager
        |
        | SHA-256 + Course validation + courseId + SemVer + minimumCoreVersion
        | atomic install / rollback
        v
CourseContentStore
        |
        | compare valid local versions
        +--> installed update only when newer than APK asset
        |
        `--> bundled APK asset when equal/newer or installed is invalid
```

کلاس‌های اصلی:

- `HttpsJsonContentUpdateProvider`: دریافت Metadata و Course Package فقط از HTTPS با Redirect محدود.
- `CourseContentUpdater`: ابتدا SemVer و `minimumCoreVersion` را از Metadata بررسی می‌کند و فقط اگر Release قابل نصب و جدیدتر باشد Package بزرگ را دانلود می‌کند.
- `FileCourseUpdateManager`: کنترل SHA-256، `courseId`، نسخه، `minimumCoreVersion`، نصب Atomic و Backup/Rollback؛ تصمیم Metadata را روی Manifest واقعی Package دوباره بررسی می‌کند.
- `CourseContentStore`: در Launch هم Asset APK و هم Package نصب‌شده را Validate و مقایسه می‌کند و **جدیدترین نسخه معتبر محلی** را انتخاب می‌کند.

فرمت Metadata کانال:

```json
{
  "courseId": "basic",
  "version": "1.1.1",
  "minimumCoreVersion": "1.4.0",
  "sha256": "<64-hex-sha256>",
  "downloadUrl": "https://.../basic-course.json"
}
```

قواعد ایمنی:

- Update فقط روی HTTPS انجام می‌شود.
- اگر Metadata همان نسخه فعال، Downgrade یا Course نیازمند Core جدیدتر را گزارش کند، Package بزرگ اصلاً دانلود نمی‌شود.
- SHA-256 قبل از فعال‌سازی Package بررسی می‌شود.
- Package باید با Validator رسمی Core معتبر باشد.
- `courseId` باید دقیقاً با Host یکسان باشد.
- Downgrade مسدود است.
- اگر Course به Core جدیدتری نیاز داشته باشد نصب نمی‌شود.
- تصمیم Version/Core پس از دانلود روی Manifest واقعی Package دوباره اجرا می‌شود؛ Metadata مرجع نهایی اعتماد نیست.
- فایل فعال به‌صورت Atomic جایگزین می‌شود و Backup برای Rollback نگه داشته می‌شود.
- اگر نسخه نصب‌شده خراب یا ناخوانا باشد، `CourseContentStore` آن را از مسیر فعال خارج می‌کند و Asset آفلاین معتبر داخل APK را نمایش می‌دهد.
- اگر APK بعدی Course هم‌نسخه یا جدیدتری نسبت به Runtime Package قبلی Bundle کند، Asset APK برنده می‌شود و Runtime Package قدیمی از مسیر فعال خارج می‌شود؛ بنابراین App Update در حالت Offline به Course قدیمی عقب نمی‌رود.
- اگر Asset APK به هر دلیل نامعتبر باشد ولی Package نصب‌شده معتبر وجود داشته باشد، نسخه نصب‌شده حفظ می‌شود تا آموزش‌ها از دسترس خارج نشوند.
- Progress، تنظیمات، Quiz History، Draftها و سایر داده‌های کاربر در Room/DataStore از Course Package جدا هستند و با Content Update حذف نمی‌شوند.

بنابراین App Update و Content Update دو چرخه مستقل دارند:

```text
App Update      = APK / package / native runtime / UI / Core version
Content Update  = lesson / quiz / exercise / project / glossary / curriculum data
```

## امکانات موجود در نسخه 1.4.0

- JSON Course Contract با Stable ID، SemVer، Schema Version و Capability flags
- Course Validator و Compiler مشترک برای CI و Runtime
- Dynamic Lesson Renderer برای متن، لیست، جدول، کد، Callout، Asset و لینک فعالیت
- Progress، «ادامه یادگیری»، قفل زنجیره‌ای Levelها و Quiz scoring همراه weak-topic analysis
- Exercise draft/evaluator، Project progress و Achievement rules
- ثبت تکمیل Exercise/Project با Repository مشترک و حضور در Backup/Restore
- Offline FTS Search، Bookmark و User Notes
- Room Database چنددوره‌ای v4 با Migration غیرمخرب `1 -> 2 -> 3 -> 4`
- اتصال خودکار داده‌های تک‌دوره‌ای قدیمی به Course جدید هنگام Import بر اساس Stable ID
- DataStore برای Theme، اعلان، اندازه متن و Profile
- Navigation، App Shell، Drawer راست، Settings، About و Branding پویا
- Backup/Restore تراکنشی schema v3 با خواندن سازگار Backupهای v1/v2
- Content Update با SHA-256، نصب Atomic و Rollback
- HTTPS runtime content channel با Metadata استاندارد و Preflight قبل از دانلود
- Newest-valid-local selection بین Runtime Package و APK Asset با `CourseContentStore`
- Code Runner plugin contract برای Adapterهای اختصاصی زبان
- WorkManager study reminder با مدیریت مجوز Android 13+
- `SpacedReviewEngine` و Flashcard generation از Glossary بدون duplication محتوا
- Flashcard Review persistence با برنامه مرور محفوظ در Update و Backup/Restore
- `WeakTopicReviewEngine` برای تبدیل weakTags آزمون‌ها به درس‌های اولویت‌دار مرور
- `PlacementEngine` با Policy قابل تنظیم و Policy چهارسطحی استاندارد
- UI مشترک Flashcard Review، Weak Topic Review و Placement Summary
- Repository عمومی برای Placement و Weak Review مبتنی بر Quiz history ذخیره‌شده
- Routeهای مشترک Placement، Weak Topic Review و Flashcard Review در `AcademyNavHost`
- انتخاب اولین درس سطح پیشنهادی از `LearningPathEngine` بدون تکرار ترتیب در Course Host
- Flashcard Session snapshot با Batch پیش‌فرض 20 کارتی و محاسبه UTC review day در Core
- `AcademyLearningCatalogScreen` برای جست‌وجو و مرور یکپارچه Quiz، Exercise و Project
- Route عمومی `academy/catalog` و helper مشترک `openLearningCatalog()`
- فیلتر All/Quiz/Exercise/Project، شمارنده و metadata استاندارد هر Activity در Catalog
- Sample App compile integration برای Catalog عمومی
- Unit/Regression Test و GitHub Actions

Learning Catalog هیچ تغییر Room یا Backup Schema ایجاد نمی‌کند؛ فقط از مدل‌های موجود `CourseBundle` استفاده می‌کند و از Routeهای عمومی Quiz/Exercise/Project به مقصد واقعی می‌رود.

Runtime Content Update نیز Database Schema را تغییر نمی‌دهد. محتوای آموزشی فایل مستقل است و داده‌های کاربر در پایگاه داده مشترک حفظ می‌شوند.

محدوده دقیق بخش‌های آماده و کارهای باقی‌مانده در [وضعیت پیاده‌سازی](docs/implementation-status.md) ثبت شده است؛ این سند اجازه نمی‌دهد قابلیت نیمه‌کاره به اشتباه Production-ready اعلام شود.

## مستندات

- [معماری](docs/architecture.md)
- [شرح ماژول‌ها](docs/modules.md)
- [قرارداد Course Package](docs/course-contract.md)
- [استفاده از Core](docs/core-usage.md)
- [اتصال یک Course Repository](docs/integration-guide.md)
- [Migration و حفاظت از داده](docs/migrations.md)
- [استاندارد کدنویسی](docs/coding-standard.md)
- [وضعیت پیاده‌سازی](docs/implementation-status.md)
- [تاریخچه تغییرات](CHANGELOG.md)

## نسخه‌بندی

نسخه App، Core، Database، Course Schema، Course Content و Curriculum مستقل‌اند. سازگاری با `minimumCoreVersion` و `contentSchemaVersion` کنترل می‌شود و تغییر Stable ID بعد از انتشار ممنوع است.

هر تغییر محتوایی که قرار است از کانال Runtime به کاربران برسد باید `manifest.version` دوره را افزایش دهد؛ وگرنه Update Planner آن را نسخه فعلی تشخیص می‌دهد و دوباره نصب نمی‌کند.
