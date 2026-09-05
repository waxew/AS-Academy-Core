# وضعیت پیاده‌سازی AS Academy Foundation

آخرین بازبینی: 2026-09-06 — Core baseline: `1.5.0`

این سند فقط وضعیت Foundation مشترک سه Repository را ثبت می‌کند و ownership را با `integration/contract.json` هماهنگ نگه می‌دارد.

| بخش | وضعیت | مالک / توضیح |
|---|---|---|
| Shared Contract v1 | تکمیل | byte-identical در Core/MainUi/MainCourse و enforce در CI |
| Core public API 1.5.0 | تکمیل | version baseline و compatibility range رسمی |
| `AcademyRuntime` composition root | تکمیل | Core؛ database/repositories/settings/reminder/backend از یک نقطه ساخته می‌شوند |
| Public domain/read models | تکمیل | Core؛ MainUi به persistence entity به‌عنوان presentation contract وابسته نیست |
| Room database | تکمیل پایه | Core؛ schema v4 و migrationهای غیرمخرب موجود |
| User-state repositories | تکمیل | Core؛ Progress/Bookmark/Notes/Search/Achievement/Quiz/Draft/Project/Completion |
| Settings / notifications | تکمیل پایه | Core؛ DataStore و WorkManager scheduler |
| Backend boundary | تکمیل | Core؛ `AcademyBackend` + Auth/Sync/Storage gateways + offline default |
| Supabase provider | پیکربندی نشده | هیچ پروژه Supabase مشخصی برای AS Academy به Foundation متصل نشده؛ اتصال provider نباید با حدس به پروژه دیگری انجام شود |
| Runtime Content Update | تکمیل | Core؛ HTTPS, SemVer, minimumCoreVersion, SHA-256, atomic install/rollback و local fallback |
| Backup/Restore | تکمیل پایه | Core؛ user state مستقل از Course package |
| Compose/UI ownership | تکمیل | MainUi؛ presentation قدیمی از Core حذف شده |
| Design System / Screens / Navigation | تکمیل در baseline | MainUi؛ توسعه UX بعدی مستقل از Core runtime انجام می‌شود |
| MainCourse content-only boundary | تکمیل | MainCourse؛ manifestهای canonical schema v1 validate می‌شوند |
| Compatibility Matrix | تکمیل | مستند و machine-readable در Foundation 1.5 |
| Cross-repo CI | تکمیل | Contract compare + validators + Core test + MainUi test + reference app assemble |
| Reference App | تکمیل | `AS-Academy-MainUi/academy-viewer`؛ sample-app قدیمی Core حذف شده |
| End-to-end build gate | تکمیل | reference app با Core + MainUi + MainCourse واقعی assemble می‌شود |
| Maven/GitHub Packages publication | هنوز فعال نشده | Composite Build روش رسمی داخلی است؛ coordinate `com.asdevelopers.academy:core:1.5.0` baseline قرارداد است ولی انتشار artifact باید در release pipeline جداگانه فعال شود |
| Release/tag Foundation 1.5 | در انتظار gate نهایی | فقط بعد از سبز شدن main هر سه repo انجام می‌شود |

## مواردی که عمداً جزو نقص Foundation محسوب نمی‌شوند

این موارد featureهای بعدی محصول هستند و blocker یکپارچگی سه Repository نیستند:

- provider مشخص Cloud Sync تا زمانی که backend واقعی انتخاب و credential/config آن مشخص نشده باشد؛
- Code Runner اختصاصی هر زبان؛
- UXهای تخصصی‌تر Course-specific؛
- analytics/streak/mastery توسعه‌یافته؛
- providerهای media/cache اختصاصی.

## Definition of Done برای Foundation 1.5

Foundation زمانی Final است که:

1. Contract هر سه `main` یکسان باشد؛
2. Core هیچ Compose presentation قدیمی نگه ندارد؛
3. MainUi implementationهای Room/backend را نسازد؛
4. MainCourse runtime implementation نداشته باشد؛
5. Core quality gates سبز باشد؛
6. MainUi quality gates روی baseline جدید Core سبز باشد؛
7. MainCourse contract gate سبز باشد؛
8. Foundation Integration سه-repo و `academy-viewer` سبز باشد؛
9. PR نهایی Core merge شود؛
10. baseline/tag release تنها پس از verify شدن `main` ایجاد شود.
