# استاندارد کدنویسی AS Academy

این سند برای تمام اپ‌های آموزشی الزامی است.

## قواعد
1. Kotlin زبان اصلی Android است.
2. Jetpack Compose رابط استاندارد است.
3. منطق مشترک فقط در AS-Academy-Core قرار می‌گیرد.
4. UI به DAO مستقیم متصل نمی‌شود.
5. Repository مرز دسترسی داده است.
6. State در ViewModel مدیریت می‌شود.
7. Coroutines/Flow برای عملیات async استفاده می‌شود.
8. داده کاربر در Migrationها حفظ می‌شود.
9. متن UI Hard-code نمی‌شود و از Resource استفاده می‌کند.
10. RTL، Dark/Light و Accessibility از ابتدا رعایت می‌شوند.
11. Permissionها حداقلی هستند.
12. Secret و Signing Key وارد ریپوی عمومی نمی‌شود.
13. هر فایل مهم توضیح نقش فایل دارد و بخش‌های غیرشفاف کد کامنت‌گذاری می‌شوند.
14. کامنت باید هدف و چرایی را توضیح دهد؛ کامنت بی‌ارزش برای دستور بدیهی تولید نشود.
15. شناسه‌های Course Content پس از انتشار پایدار می‌مانند.

## نام‌گذاری Package
Core: `com.asdevelopers.academy.core`
Standalone apps: `com.asdevelopers.academy.<course>`
All-in-One: `com.asdevelopers.academy`

## Definition of Done
- Build موفق
- Lint/Test موفق
- Content validation موفق
- RTL بررسی شده
- Dark/Light بررسی شده
- Offline flow بررسی شده
- Migration بدون حذف Progress
- README مشخص می‌کند چه بخش‌هایی از Core استفاده شده‌اند
