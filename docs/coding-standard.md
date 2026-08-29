# استاندارد کدنویسی AS Academy

## مرز مسئولیت

1. کد قابل استفاده در بیش از یک Course فقط در Core قرار می‌گیرد.
2. UI مستقیم به DAO یا فایل JSON متصل نمی‌شود؛ مسیر استاندارد Repository و State holder است.
3. ماژول‌های JVM نباید Android import داشته باشند.
4. Course فقط Content، Branding، Capability و Adapter اختصاصی نگه می‌دارد.

## کیفیت کد

1. Kotlin و Jetpack Compose فناوری استاندارد Android هستند.
2. عملیات async با Coroutines/Flow انجام می‌شود.
3. هر فایل عمومی KDoc نقش خود را توضیح می‌دهد و Comment باید دلیل تصمیم غیرشفاف را بیان کند.
4. Stable ID، Version و Error type به جای رشته یا عدد پراکنده استفاده می‌شوند.
5. API عمومی باید ورودی نامعتبر را با نتیجه تایپ‌شده یا خطای واضح رد کند.
6. Secret، Token، Signing Key و اطلاعات شخصی وارد Repository عمومی نمی‌شوند.

## UI و دسترس‌پذیری

1. RTL و LTR هر دو باید قابل استفاده باشند؛ Drawer مشترک در RTL از راست باز می‌شود.
2. Light/Dark و Font scale از تنظیمات مشترک می‌آیند.
3. تصویر و Diagram باید accessibility label داشته باشند.
4. متن‌های مشترک باید در Core متمرکز و برای Localisation قابل انتقال به Resource باشند؛ Course متن UI عمومی را Fork نمی‌کند.
5. Permission فقط هنگام نیاز و با توضیح قابل فهم درخواست می‌شود.

## داده و نسخه

1. Migration مخرب داده کاربر ممنوع است.
2. Stable ID منتشرشده تغییر نمی‌کند.
3. تغییر Contract با Template، Sample، Validator، تست و مستندات هم‌زمان Commit می‌شود.
4. نسخه App، Core، DB، Course Schema و Content مستقل نگه داشته می‌شوند.

## Definition of Done

- Unit Testهای `course` و `engine` موفق‌اند.
- Course Template با CLI معتبر و قابل Compile است.
- `core:lintDebug` و `sample-app:assembleDebug` موفق‌اند.
- مسیر Offline، RTL، Dark/Light و Back navigation بررسی شده است.
- Migration یا Compatibility تغییر یافته تست و مستند شده است.
- README Course نسخه Core و Adapterهای اختصاصی را اعلام می‌کند.
