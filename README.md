# AS Academy Core

هسته مرکزی تمام اپلیکیشن‌های آموزشی AS Academy.

این ریپو مرجع واحد معماری، اسکلت اپ، قرارداد Course Package، اجزای UI مشترک و موتورهای عمومی آموزش است. هیچ پروژه آموزشی نباید منطق مشترک را دوباره پیاده‌سازی کند؛ تغییرات عمومی باید ابتدا در این ریپو انجام شوند و اپ‌های دوره‌ای فقط محتوای آموزشی، برندینگ و قابلیت‌های اختصاصی خود را نگه دارند.

## اصول اصلی
- Shared Core برای تمام دوره‌ها
- Offline First
- Course Package مستقل از منطق برنامه
- Stable IDs برای محتوا
- No Data Loss در Migrationها
- Reusable UI
- نسخه مستقل App/Core/DB/Content
- RTL First
- سورس کامنت‌گذاری‌شده

## پروژه‌های مصرف‌کننده
JavaScript، Python، Java، C، C++، C#، PHP، Kotlin و تمام دوره‌های بعدی Academy Learn.

## ساختار مرجع
```text
AS-Academy-Core/
├── app-shell/
├── core/
│   ├── design-system/
│   ├── navigation/
│   ├── database/
│   ├── content-engine/
│   ├── progress-engine/
│   ├── quiz-engine/
│   ├── exercise-engine/
│   ├── project-engine/
│   ├── search-engine/
│   ├── bookmark-engine/
│   ├── glossary-engine/
│   ├── achievement-engine/
│   ├── code-runner/
│   ├── update-engine/
│   ├── backup-engine/
│   ├── settings/
│   └── profile/
├── course/
│   ├── schema/
│   ├── sample-course/
│   └── template/
├── docs/
├── tools/
└── testing/
```

## مرز مسئولیت
### Core
Navigation، Design System، Room/Database، Content Import، Progress، Quiz، Exercise، Project، Search، Bookmark، Glossary، Profile/Drawer، Settings، Achievement، Update، Backup، Code Runner Framework، Schema و Validation.

### ریپوی هر دوره
Course Package، درس‌ها، تمرین‌ها، آزمون‌ها، پروژه‌ها، واژه‌نامه، تصاویر، Branding و قابلیت اختصاصی دوره.

## قانون تغییرات
اگر یک تغییر برای بیش از یک اپ آموزشی قابل استفاده است، محل آن `AS-Academy-Core` است. اگر فقط مربوط به یک دوره است، در ریپوی همان دوره باقی می‌ماند.

مستند معماری کامل: `docs/architecture.md`
قرارداد Course: `docs/course-contract.md`
اصول کدنویسی: `docs/coding-standard.md`
