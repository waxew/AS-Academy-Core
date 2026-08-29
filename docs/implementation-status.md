# وضعیت پیاده‌سازی AS Academy Core

آخرین بازبینی: 2026-08-30 — نسخه Core: `1.0.0`

این جدول بین «API و پیاده‌سازی پایه موجود» و «آماده برای همه سناریوهای Production» تفاوت می‌گذارد.

| بخش | وضعیت | توضیح |
|---|---|---|
| Course models/serialization | پیاده‌سازی شده | Manifest، Branding، Level، Chapter، Lesson، Activity و Resource |
| Codec/Validator/CLI | پیاده‌سازی شده | Validate پوشه و Compile به Bundle واحد |
| Progress Engine | پیاده‌سازی و Unit Test | محاسبه Block progress و Summary |
| Learning Path | پیاده‌سازی و Unit Test | Continue Learning، درصد Course/Level و قفل زنجیره‌ای Levelها با آستانه قابل تنظیم |
| Activity completion | پیاده‌سازی شده | تکمیل Exercise/Project در Room، Repository و Backup/Restore |
| Quiz Engine | پیاده‌سازی و Unit Test | سؤال‌های انتخابی، Fill Code، Ordering، Matching و weak-topic analysis |
| Quiz UI | پیاده‌سازی پایه | Screen مشترک برای انواع QuestionType، ثبت نتیجه از Callback؛ History persistence باید توسط Host به Repository متصل شود |
| Exercise | پیاده‌سازی پایه | مدل، Draft، Expected-output evaluator و Screen مشترک پاسخ/Hint/Solution؛ Runner واقعی هر زبان Plugin است |
| Project/Achievement | پیاده‌سازی پایه | مدل، persistence، rule evaluation و Screen مشترک Milestone/Draft؛ UX پیشرفته پروژه باقی مانده |
| Search/Bookmark/Notes | پیاده‌سازی شده | Room FTS، Repository و مدل‌های چنددوره‌ای؛ Screenهای کامل مدیریت هنوز باقی مانده‌اند |
| Room database | پیاده‌سازی شده | Schema v3، Migration غیرمخرب 1→2→3، تشخیص دو شکل v2 و اتصال خودکار داده Legacy؛ تست Instrumented Migration باید افزوده شود |
| Settings/Profile/Drawer | پیاده‌سازی شده | DataStore، Theme/Font scale، URI پایدار تصویر، thumbnail و Drawer راست |
| Navigation/App Shell/Theme | پیاده‌سازی شده | Routeهای Home/Settings/About/Lesson/Quiz/Exercise/Project، Slot مسیرهای اختصاصی و Branding پویا |
| Lesson Renderer | پیاده‌سازی پایه | Blockهای عمومی و لینک واقعی Quiz/Exercise/Project از Callback؛ Asset renderer از Host تزریق می‌شود |
| Backup/Restore | پیاده‌سازی و Unit Test قرارداد | Schema v2، خواندن سازگار v1 و Restore تراکنشی |
| Content Update | پیاده‌سازی شده | SHA-256، SemVer، نصب Atomic و Rollback فایل محلی |
| Notifications | پیاده‌سازی پایه | WorkManager و بررسی مجوز Android 13+ |
| Sample App | موجود | Loader، Shell، Settings و Lesson Renderer را اجرا می‌کند؛ نمونه اتصال Screenهای Activity باید تکمیل شود |
| CI | موجود | JVM tests، Template validation، Android lint و Sample build |
| انتشار Maven/GitHub Packages | باقی مانده | فعلاً Composite Build روش رسمی مصرف است |
| Cloud Sync/App Update service | خارج از نسخه 1.0 | API سرویس و سیاست Conflict هنوز طراحی نشده |
| Code runners واقعی | مختص Course | فقط Contract/Registry عمومی در Core است |

## معیار انتقال قابلیت از Course به Core

اگر یک قابلیت در حداقل دو Course قابل استفاده است، Issue آن باید در Core باز و API عمومی آن اینجا پیاده‌سازی شود. Course می‌تواند تا زمان انتشار Core یک Adapter محدود داشته باشد، اما Fork دائمی Engine یا UI عمومی پذیرفته نیست.

## گام‌های کیفیت بعدی

1. افزودن Room Migration instrumentation test و Commit schema JSON تولیدشده
2. تکمیل persistence wiring و UIهای Search، Bookmark و Notes
3. افزودن Asset loader تصویری/صوتی با cache و accessibility
4. انتشار Artifact نسخه‌دار برای جایگزینی Composite Build در Release CI
5. افزودن API اختیاری Cloud Sync بدون شکستن Offline-first
6. افزودن UI تخصصی‌تر برای Code Runner و ارزیابی خودکار Exerciseهای زبان‌محور
