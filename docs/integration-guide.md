# راهنمای اتصال یک Course Repository

این Checklist برای JavaScript و تمام دوره‌های بعدی یکسان است.

## 1. وابستگی

- Core را به‌صورت checkout مجاور یا submodule اضافه کنید.
- `includeBuild` و Dependency مختصات `com.asdevelopers.academy:core:1.0.0` را تنظیم کنید.
- نسخه محلی Navigation، Database، Theme یا Engineها را از Course حذف کنید.

## 2. محتوا

- Template را کپی کنید، نه کد Runtime را.
- یک `courseId` کوتاه، lowercase و پایدار انتخاب کنید؛ نمونه: `javascript`.
- Prefix همه Stable IDها را یکسان نگه دارید؛ نمونه: `js-fnd-001`.
- `minimumCoreVersion` را بر اساس API واقعاً مصرف‌شده بنویسید.
- هر بار قبل از Commit، `:tools:run --args="validate ..."` را اجرا کنید.

## 3. Android Host

- Bundle ساخته‌شده را داخل `app/src/main/assets/` قرار دهید.
- `CoursePackageLoader` را در startup یا ViewModel اجرا کنید.
- Branding خوانده‌شده را به `AcademyTheme` بدهید.
- `AcademyDatabase` و Repositoryهای Core را از DI یا Application scope بسازید.
- `CoursePackageImporter.import` را پس از نصب یا تغییر Content Version اجرا کنید؛ FTS و اتصال داده Legacy هر دو همان‌جا انجام می‌شوند.
- Dashboard و مقصد «ادامه یادگیری» را از `ProgressRepository.observeDashboard` بگیرید و قانون Level را در Host تکرار نکنید.
- مجوز اعلان Android 13+ را فقط پس از فعال‌کردن Reminder توسط کاربر درخواست کنید.

## 4. انتقال کد قدیمی JavaScript

| کد موجود در پروژه JavaScript | مقصد درست |
|---|---|
| Navigation/Drawer/Settings/About | حذف و مصرف `core` |
| Entity/DAO/Progress/Bookmark/Note | حذف و مصرف Repositoryهای `core` |
| Quiz scoring و weak topics | حذف و مصرف `QuizEngine` |
| Lesson UI عمومی | تبدیل محتوا به LessonBlock و مصرف `LessonRenderer` |
| اجرای واقعی JavaScript | نگهداری Adapter اختصاصی `CodeRunnerPlugin` |
| درس‌ها و مثال‌های JavaScript | نگهداری در Course Package |
| رنگ، لوگو و تصاویر JavaScript | نگهداری در Branding/Assets همان Course |

## 5. کنترل نهایی

- اپ بدون اینترنت درس و Progress را باز می‌کند.
- Content نامعتبر قبل از UI رد می‌شود.
- Stable IDهای منتشرشده تغییر نکرده‌اند.
- Upgrade دیتابیس داده کاربر را حفظ می‌کند.
- Dark/Light، RTL، اندازه متن و Back navigation بررسی شده‌اند.
- هیچ Secret یا Signing Key داخل Repository عمومی نیست.
