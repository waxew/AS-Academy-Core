# وضعیت پیاده‌سازی AS Academy Core

آخرین بازبینی: 2026-09-06 — نسخه Core: `1.4.0`

این جدول بین «API و پیاده‌سازی پایه موجود» و «آماده برای همه سناریوهای Production» تفاوت می‌گذارد.

| بخش | وضعیت | توضیح |
|---|---|---|
| Course models/serialization | پیاده‌سازی شده | Manifest، Branding، Level، Chapter، Lesson، Activity و Resource |
| Codec/Validator/CLI | پیاده‌سازی شده | Validate پوشه و Compile به Bundle واحد |
| Progress Engine | پیاده‌سازی و Unit Test | محاسبه Block progress و Summary |
| Learning Path | پیاده‌سازی و Unit Test | Continue Learning، درصد Course/Level، قفل زنجیره‌ای و first-lesson lookup برای Placement |
| Activity completion | پیاده‌سازی شده | تکمیل Exercise/Project در Room، Repository و Backup/Restore |
| Quiz Engine | پیاده‌سازی و Unit Test | سؤال‌های انتخابی، Fill Code، Ordering، Matching و weak-topic analysis |
| Quiz UI | پیاده‌سازی پایه | Screen مشترک برای انواع QuestionType و ثبت نتیجه از Callback؛ History persistence از Repository مشترک مصرف می‌شود |
| Learning Catalog | پیاده‌سازی شده | Search و Filter مشترک Quiz/Exercise/Project، metadata/card استاندارد، Route عمومی و Callback به Activity Screenها |
| Placement | پیاده‌سازی و Unit Test | Policy قابل تنظیم، Policy چهارسطحی، Repository نتیجه Persist شده، Route و Placement Summary UI |
| Weak Topic Review | پیاده‌سازی و Unit Test | تحلیل persisted weakTags، Repository Course-scoped، Route و Screen مشترک |
| Spaced Review / Flashcard | پیاده‌سازی و Unit Test | Glossary -> Flashcard، Scheduling، Due selection، Session snapshot/batch، Room، Backup/Restore و Review Screen |
| Exercise | پیاده‌سازی پایه | مدل، Draft، Expected-output evaluator و Screen مشترک پاسخ/Hint/Solution؛ Runner واقعی هر زبان Plugin است |
| Project/Achievement | پیاده‌سازی پایه | مدل، persistence، rule evaluation و Screen مشترک Milestone/Draft؛ UX پیشرفته پروژه باقی مانده |
| Search/Bookmark/Notes | پیاده‌سازی شده | Room FTS، Repository و مدل‌های چنددوره‌ای؛ Screenهای کامل مدیریت در MainUi تکمیل می‌شوند |
| Room database | پیاده‌سازی شده | Schema v4 و Migration غیرمخرب 1→2→3→4 موجود است؛ Instrumented Migration Test و commit schema JSON هنوز باید تکمیل شود |
| Runtime composition | پیاده‌سازی شده | `AcademyRuntime` مالک ساخت Database، Repositoryها، Preferences و Reminder است؛ MainUi و Course Hostها نباید persistence graph موازی بسازند |
| Settings/Profile/Drawer | پیاده‌سازی شده | DataStore، Theme/Font scale، URI پایدار تصویر، thumbnail و Drawer راست |
| Navigation/App Shell/Theme | پیاده‌سازی شده | Home/Settings/About/Lesson/Quiz/Exercise/Project + Placement/Weak Review/Flashcard Review/Learning Catalog و Branding پویا |
| Lesson Renderer | پیاده‌سازی پایه | Blockهای عمومی و لینک واقعی Quiz/Exercise/Project از Callback؛ Asset renderer از Host تزریق می‌شود |
| Backup/Restore | پیاده‌سازی و Unit Test قرارداد | Schema v3، خواندن سازگار v1/v2، Flashcard Progress و Restore تراکنشی |
| Content Update | پیاده‌سازی شده | Core 1.4.0: HTTPS metadata، SemVer/minimumCoreVersion preflight، SHA-256، نصب Atomic، Rollback و newest-valid-local selection |
| Notifications | پیاده‌سازی پایه | WorkManager و بررسی مجوز Android 13+ |
| Sample App | موجود و یکپارچه | Loader/Shell/Settings/Lesson + Quiz persistence + Placement + Weak Review + Flashcard Review؛ Catalog API نیز در sample-app compile می‌شود |
| CI | موجود | JVM tests، Template validation، Android lint و Sample build؛ integration gate سه‌ریپو در MainUi نیز اضافه شده است |
| انتشار Maven/GitHub Packages | باقی مانده | فعلاً Composite Build روش رسمی مصرف است |
| Cloud Sync/App Update service | خارج از 1.4 | API سرویس و سیاست Conflict هنوز طراحی نشده |
| Code runners واقعی | مختص Course | فقط Contract/Registry عمومی در Core است |

## قرارداد مالکیت بین سه Repository

- **Core** مالک Engine، Database، Repositoryها، Runtime composition، Navigation contract، Update/Backup و state پایدار کاربر است.
- **MainUi** مالک Design System، Screenها و presentation است و Runtime را از Core مصرف می‌کند؛ ساخت Database/Repository مستقل در MainUi مجاز نیست.
- **MainCourse** تنها منبع قابل ویرایش محتوای دوره‌ها است و هر `courses/<id>/course` باید با Validator همان Core که Host مصرف می‌کند معتبر باشد.
- Course App فقط Host نازک، application id، signing، branding/capability اختصاصی و Adapter واقعاً زبان‌محور را نگه می‌دارد.

## معیار انتقال قابلیت از Course به Core/MainUi

اگر یک قابلیت در حداقل دو Course قابل استفاده است، منطق/state آن باید در Core و presentation مشترک آن باید در MainUi قرار گیرد. Course می‌تواند Adapter واقعاً اختصاصی داشته باشد، اما Fork دائمی Engine، Repository، Navigation یا UI عمومی پذیرفته نیست.

## گام‌های کیفیت بعدی

1. افزودن Room Migration instrumentation test و Commit schema JSON تولیدشده
2. انتقال کامل route/back-stack اختصاصی Folder Host به Navigation contract مرکزی Core
3. تکمیل Asset loader تصویری/صوتی با cache و accessibility
4. انتشار Artifact نسخه‌دار برای جایگزینی Composite Build در Release CI
5. افزودن API اختیاری Cloud Sync بدون شکستن Offline-first
6. افزودن UI تخصصی‌تر برای Code Runner و ارزیابی خودکار Exerciseهای زبان‌محور
7. اضافه‌کردن آمار Review/Streak و Mastery Dashboard روی Engineهای موجود
