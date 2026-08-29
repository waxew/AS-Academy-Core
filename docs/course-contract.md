# قرارداد Course Package

مدل‌های موجود در ماژول `course` و `engine` منبع نهایی قراردادند. این سند شکل فایل‌های قابل ویرایش را توضیح می‌دهد؛ CLI آن‌ها را به یک `bundle.json` قابل مصرف Android تبدیل می‌کند.

## ساختار پوشه

```text
course-content/
├── manifest.json
├── branding.json
├── levels.json
├── chapters.json
├── assets.json
├── references.json
├── lessons/
│   └── <lesson-id>.json
├── quizzes/
│   └── <quiz-id>.json
├── exercises/
│   └── <exercise-id>.json
├── projects/
│   └── <project-id>.json
├── glossary/
│   └── glossary.json
└── files/
    └── ... assets referenced by relativePath
```

چهار فایل `manifest.json`، `branding.json`، `levels.json` و `chapters.json` الزامی‌اند. پوشه‌های فعالیت و فایل‌های Resource در صورت غیرفعال‌بودن Capability می‌توانند خالی باشند. یک Course قابل انتشار باید حداقل یک Level، Chapter و Lesson داشته باشد.

## Manifest

```json
{
  "courseId": "javascript",
  "titleFa": "آموزش JavaScript",
  "titleEn": "JavaScript",
  "version": "1.0.0",
  "curriculumVersion": "1.0.0",
  "contentSchemaVersion": 1,
  "minimumCoreVersion": "1.0.0",
  "publisherId": "as-team",
  "rtl": true,
  "defaultLocale": "fa",
  "supportedLocales": ["fa"],
  "capabilities": {
    "codeRunner": true,
    "terminalExamples": false,
    "diagrams": true,
    "quizzes": true,
    "exercises": true,
    "projects": true,
    "glossary": true,
    "bookmarks": true,
    "userNotes": true,
    "achievements": true,
    "offlineContent": true
  }
}
```

`version` انتشار فایل‌های محتوا و `curriculumVersion` تغییر مسیر آموزشی را نشان می‌دهد. `contentSchemaVersion` شکل JSON و `minimumCoreVersion` حداقل Runtime سازگار را مشخص می‌کند.

## Stable ID

ID فقط از حروف کوچک انگلیسی، عدد و خط تیره ساخته می‌شود. پس از انتشار تغییر ID به معنی رکورد جدید است و Progress، Bookmark یا Note قبلی دیگر به آن متصل نمی‌شود.

نمونه:

- Level: `js-fundamentals`
- Chapter: `js-fundamentals-syntax`
- Lesson: `js-fnd-001`
- Quiz: `js-qz-001`
- Exercise: `js-ex-001`
- Project: `js-prj-001`

Order در هر Parent باید یکتا و صفر یا مثبت باشد. Reference به Level، Chapter، Lesson، Asset و Prerequisite باید به ID موجود اشاره کند.

## Lesson Block

نوع‌های پشتیبانی‌شده:

`TITLE`, `SUBTITLE`, `PARAGRAPH`, `LIST`, `TABLE`, `IMAGE`, `DIAGRAM`, `CODE`, `OUTPUT`, `TIP`, `WARNING`, `NOTE`, `IMPORTANT`, `EXERCISE`, `QUIZ`, `PROJECT_LINK`, `REFERENCE`.

`content` متن اصلی است. Metadata فقط اطلاعات نوع Block را نگه می‌دارد؛ برای نمونه `language` در CODE، `assetId` در IMAGE، `quizId`، `exerciseId` یا `projectId` در Activity link. مقدار قدیمی `PROJECT` فقط برای خواندن محتوای آزمایشی قبلی باقی مانده و در فایل جدید استفاده نمی‌شود.

## Asset و امنیت مسیر

Asset با `relativePath`، MIME type، نوع، توضیح دسترس‌پذیری و SHA-256 اختیاری معرفی می‌شود. مسیر مطلق یا دارای `..` رد می‌شود. فایل Release باید Hash خارج از Bundle نیز داشته باشد تا `FileCourseUpdateManager` قبل از Decode دستکاری یا دانلود ناقص را تشخیص دهد.

Compiler فعلی JSONها را به `bundle.json` تبدیل می‌کند؛ فایل‌های باینری `files/` باید همراه Bundle در assets یا بسته دانلودی Course قرار گیرند و مسیر ثبت‌شده در `assets.json` را حفظ کنند.

## Versioning محتوا

- `MAJOR`: تغییر ناسازگار برنامه آموزشی یا حذف گسترده محتوا
- `MINOR`: Level، Chapter، Lesson یا قابلیت جدید
- `PATCH`: اصلاح متن، مثال، لینک یا اشکال محتوا بدون تغییر Stable ID

Downgrade به‌صورت پیش‌فرض توسط Update Planner رد می‌شود. Package نیازمند Core جدید نیز قبل از نصب رد می‌شود.

## Validate و Compile

```bash
./gradlew :tools:run --args="validate course/template"
./gradlew :tools:run --args="compile course/template build/course.json"
```

خطاها Release را متوقف می‌کنند؛ Warning مانند Capability فعال با لیست خالی باید پیش از انتشار بررسی شود، ولی برای Draft مانع Compile نیست.
