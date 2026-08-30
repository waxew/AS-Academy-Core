# AS Academy Core

اسکلت مرکزی و قابل اجرای همه اپ‌های آموزشی AS Academy؛ این Repository محل واحد کدهای مشترک است تا پروژه‌های JavaScript، Python، Java و دوره‌های بعدی Navigation، Database، UI و موتورهای آموزشی را دوباره ننویسند.

نسخه فعلی فقط یک تعریف معماری یا پوشه خالی نیست: قرارداد محتوای تایپ‌شده، موتورهای JVM، Runtime اندروید، ابزار Validate/Compile، Course Template، Sample App، تست و CI در خود Repository قرار دارند.

## قانون اصلی

اگر یک قابلیت در بیش از یک دوره کاربرد دارد، باید اینجا پیاده‌سازی شود. Repository هر دوره فقط این موارد را نگه می‌دارد:

- Course Package و Assetهای همان دوره
- Branding و تنظیم Capabilityها
- Adapter واقعاً اختصاصی، مانند JavaScript Code Runner
- تنظیمات نهایی اپ، Package Name و Signing خارج از Core

کپی‌کردن Navigation، Room، Progress، Quiz، Search، Bookmark، Settings، Drawer/Profile، Lesson Renderer، Update، Backup، Placement یا Review Engine داخل Course Repository مجاز نیست.

## نسخه فعلی

- Core/API: **1.1.0**
- Course schema: **1**
- Room database schema: **4**
- Backup schema: **3**

## ماژول‌های واقعی

| ماژول | نوع | مسئولیت |
|---|---|---|
| `course` | Kotlin/JVM | قرارداد Serializable برای Manifest، محتوا، Branding و منابع |
| `engine` | Kotlin/JVM | Validator، Codec، Progress، Quiz، Exercise، Project، Search، Review، Placement، Achievement، Code Runner API، Version و Update rules |
| `core` | Android Library | Room، Repository، DataStore، WorkManager، Navigation Compose، Theme، Drawer، Renderer و Review UI |
| `tools` | JVM CLI | اعتبارسنجی پوشه Course و ساخت `bundle.json` بدون Android SDK |
| `sample-app` | Android App | نمونه اجرایی اتصال Course Package به Core |

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
# تست موتورهای مستقل از Android
./gradlew :course:test :engine:test

# اعتبارسنجی Template قابل ویرایش
./gradlew :tools:run --args="validate course/template"

# ساخت یک فایل واحد برای assets اپ Android
./gradlew :tools:run --args="compile course/template build/course-bundle.json"

# بررسی Runtime و ساخت اپ مرجع
./gradlew :core:lintDebug :sample-app:assembleDebug
```

برای شروع یک دوره، پوشه `course/template` را در Repository دوره کپی و تمام مقدارهای `replace-me` و شناسه‌های `course-*` را با شناسه‌های پایدار همان دوره جایگزین کنید. سپس با CLI بالا آن را Validate کنید.

## اتصال از Repository دوره

Core را به‌صورت Git submodule یا checkout مجاور نگه دارید و در `settings.gradle.kts` پروژه دوره معرفی کنید:

```kotlin
includeBuild("../AS-Academy-Core")
```

سپس تنها وابستگی Runtime را به اپ اضافه کنید؛ `engine` و `course` به‌صورت Transitive می‌آیند:

```kotlin
dependencies {
    implementation("com.asdevelopers.academy:core:1.1.0")
}
```

جزئیات کامل در [راهنمای مصرف Core](docs/core-usage.md) و [راهنمای اتصال یک دوره](docs/integration-guide.md) آمده است.

## امکانات موجود در نسخه 1.1.0

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
- Code Runner plugin contract برای Adapterهای اختصاصی زبان
- WorkManager study reminder با مدیریت مجوز Android 13+
- `SpacedReviewEngine` و Flashcard generation از Glossary بدون duplication محتوا
- Flashcard Review persistence با برنامه مرور محفوظ در Update و Backup/Restore
- `WeakTopicReviewEngine` برای تبدیل weakTags آزمون‌ها به درس‌های اولویت‌دار مرور
- `PlacementEngine` با Policy قابل تنظیم و Policy چهارسطحی استاندارد
- UI مشترک Flashcard Review، Weak Topic Review و Placement Summary
- Sample Course، Unit/Regression Test و GitHub Actions

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
