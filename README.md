# AS Academy Core

`AS-Academy-Core` موتور و Runtime مشترک AS Academy است. این Repository مالک منطق، persistence، runtime composition، update infrastructure و backend boundary است؛ **UI و presentation در `AS-Academy-MainUi`** و **محتوای آموزشی در `AS-Academy-MainCourse`** نگهداری می‌شوند.

## Foundation baseline

- Core/API: **1.5.0**
- Contract: **v1**
- Course schema: **1**
- Room database schema: **4**
- Backup schema: **3**
- Android: **minSdk 23 / compileSdk 36 / Java 17**

فایل machine-readable مرجع بین هر سه Repository در `integration/contract.json` قرار دارد. هر سه Repository باید نسخه byte-identical همین Contract را داشته باشند و CI این موضوع را enforce می‌کند.

## مرزبندی رسمی سه Repository

```text
AS-Academy-MainCourse  ->  content only
          |
          v
AS-Academy-Core        ->  runtime / engines / persistence / backend boundary
          |
          v
AS-Academy-MainUi      ->  design system / screens / navigation / reference app
          |
          v
Course App             ->  thin host: identity + config + course selection
```

### Core مالک است

- `AcademyRuntime` به‌عنوان composition root واحد
- Room database، DAOها و migrationها
- Repositoryهای Progress، Bookmark، Notes، Search، Achievement، Quiz History، Draft و Project Progress
- DataStore و Study Reminder
- Course codec/validator/compiler rules و engineهای آموزشی
- Runtime Content Update، SHA-256، HTTPS delivery، atomic install و rollback
- Backup/Restore
- public domain/read models برای مصرف presentation
- `AcademyBackend` و gatewayهای Auth / Sync / Storage
- provider implementationهای backend در صورت اضافه‌شدن؛ SDK یک provider مثل Supabase نباید به MainUi یا MainCourse نشت کند

### Core مالک نیست

- Compose screenها، Theme، App Shell، Drawer یا navigation UI
- محتوای درس/Quiz/Exercise/Project
- branding یا layout اختصاصی یک Course App

این بخش‌ها به‌ترتیب متعلق به `AS-Academy-MainUi`، `AS-Academy-MainCourse` و Host بسیار نازک هر اپ هستند.

## ماژول‌ها

| ماژول | نوع | مسئولیت |
|---|---|---|
| `course` | Kotlin/JVM | مدل و قرارداد Serializable محتوای Course |
| `engine` | Kotlin/JVM | Validator، Codec، SemVer، Progress، Quiz، Exercise، Project، Search، Review، Placement، Achievement و update rules |
| `core` | Android Library | persistence، repositories، runtime composition، settings، notifications، backup و content runtime |
| `tools` | JVM CLI | validate/compile رسمی Course Package بدون Android SDK |

Reference application رسمی دیگر داخل Core نیست. برنامه مرجع در `AS-Academy-MainUi/academy-viewer` قرار دارد و Foundation Integration آن را با **Core + MainUi + MainCourse واقعی** assemble می‌کند.

## مصرف Core

روش توسعه محلی استاندارد Composite Build است:

```kotlin
includeBuild("../AS-Academy-Core")
```

مختصات baseline عمومی Foundation:

```kotlin
com.asdevelopers.academy:core:1.5.0
```

تا زمانی که artifact publication رسمی فعال نشده باشد، Composite Build مرجع قابل اتکای buildهای داخلی است. Host نباید نسخه دیگری از runtime یا persistence را پیاده‌سازی کند.

## Runtime composition

مصرف‌کننده باید runtime را از Core دریافت کند:

```kotlin
val coreRuntime = AcademyRuntime.create(context)
```

MainUi نیز همین runtime را consume می‌کند و حق ساخت `AcademyDatabase`، DAO یا backend client مستقل را ندارد.

Backend به‌صورت injectable است و offline-first باقی می‌ماند:

```kotlin
val runtime = AcademyRuntime.create(
    context = context,
    backend = myAcademyBackend
)
```

`OfflineAcademyBackend` default امن برای اپی است که remote backend پیکربندی نکرده است.

## Runtime Content Update

Content و APK دو چرخه مستقل دارند. Core 1.4+ از metadata و packageهای HTTPS، SemVer، `minimumCoreVersion`، SHA-256، validation، atomic install و rollback پشتیبانی می‌کند. انتخاب محتوا در launch به‌صورت زیر است:

```text
newest valid installed content
          |
          | if newer and compatible
          v
active runtime content
          |
          `-- otherwise --> bundled APK asset fallback
```

Progress، Notes، Quiz History، Draftها و سایر داده‌های کاربر از Course Package جدا هستند و با Content Update حذف نمی‌شوند.

## Backend / Supabase rule

Foundation 1.5 به provider خاصی قفل نشده است. `AcademyBackend` مرز عمومی Core است. اگر Supabase برای AS Academy فعال شود:

- client و implementation فقط داخل Core یا یک provider module متعلق به Core قرار می‌گیرد؛
- MainUi و MainCourse هیچ import مستقیم از Supabase ندارند؛
- secret/service-role key هرگز داخل اپ یا Repository commit نمی‌شود؛
- schema/RLS و sync policy باید مستقل از presentation باقی بمانند.

## CI و Integration Gate

`Foundation Integration` در Core:

1. Core را checkout می‌کند؛
2. `main` واقعی MainUi و MainCourse را checkout می‌کند؛
3. Contract هر سه repo را byte-compare می‌کند؛
4. validator هر سه را اجرا می‌کند؛
5. Core را build/test می‌کند؛
6. MainUi را علیه همان Core build/test می‌کند؛
7. `academy-viewer` را به‌عنوان reference app end-to-end assemble می‌کند.

Merge Foundation فقط زمانی معتبر است که این gate و quality gates هر سه repository سبز باشند.

## قانون توسعه آینده

- تغییر منطق/runtime مشترک -> Core
- تغییر ظاهر/navigation/screen مشترک -> MainUi
- تغییر lesson/quiz/exercise/project/curriculum -> MainCourse
- Course App جدید -> فقط identity/config و انتخاب Course؛ بدون fork کردن Core/MainUi/MainCourse

برای جزئیات بیشتر:

- [Architecture](docs/architecture.md)
- [Modules](docs/modules.md)
- [Implementation status](docs/implementation-status.md)
- [Integration guide](docs/integration-guide.md)
- [Contract](integration/contract.json)
