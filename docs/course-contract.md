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
│   ├── fundamentals.json
│   └── ... object یا arrayهای ماژولار
├── flashcards/
│   ├── fundamentals.json
│   └── ... object یا arrayهای ماژولار
└── files/
    └── ... assets referenced by relativePath
```

چهار فایل `manifest.json`، `branding.json`، `levels.json` و `chapters.json` الزامی‌اند. پوشه‌های فعالیت و فایل‌های Resource در صورت غیرفعال‌بودن Capability می‌توانند خالی باشند. یک Course قابل انتشار باید حداقل یک Level، Chapter و Lesson داشته باشد.

`glossary/` و `flashcards/` برای Authoring بزرگ ماژولارند؛ هر فایل JSON داخل این دو پوشه می‌تواند یک Object یا یک Array از Objectها باشد. این قرارداد Conflictهای Git را در دوره‌های بزرگ کاهش می‌دهد و اجازه می‌دهد هر Level یا Chapter فایل مستقل داشته باشد.

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
    "flashcards": true,
    "bookmarks": true,
    "userNotes": true,
    "achievements": true,
    "offlineContent": true
  }
}
```

`version` انتشار فایل‌های محتوا و `curriculumVersion` تغییر مسیر آموزشی را نشان می‌دهد. `contentSchemaVersion` شکل JSON و `minimumCoreVersion` حداقل Runtime سازگار را مشخص می‌کند.

## Stable ID

ID فقط از حروف کوچک انگلیسی، عدد و خط تیره ساخته می‌شود. پس از انتشار تغییر ID به معنی رکورد جدید است و Progress، Bookmark، Note یا Flashcard Review قبلی دیگر به آن متصل نمی‌شود.

نمونه:

- Level: `js-fundamentals`
- Chapter: `js-fundamentals-syntax`
- Lesson: `js-fnd-001`
- Quiz: `js-qz-001`
- Exercise: `js-ex-001`
- Project: `js-prj-001`
- Flashcard: `js-flash-fnd-001`

Order در هر Parent باید یکتا و صفر یا مثبت باشد. Reference به Level، Chapter، Lesson، Asset و Prerequisite باید به ID موجود اشاره کند.

## Lesson Block

نوع‌های canonical پشتیبانی‌شده:

`TITLE`, `SUBTITLE`, `PARAGRAPH`, `LIST`, `TABLE`, `IMAGE`, `DIAGRAM`, `CODE`, `OUTPUT`, `TIP`, `WARNING`, `NOTE`, `IMPORTANT`, `EXERCISE`, `QUIZ`, `PROJECT_LINK`, `REFERENCE`.

`content` متن اصلی است. Metadata فقط اطلاعات نوع Block را نگه می‌دارد؛ برای نمونه `language` در CODE، `assetId` در IMAGE، `quizId`، `exerciseId` یا `projectId` در Activity link.

مقدارهای قدیمی `EXERCISE_LINK` و `PROJECT` فقط برای خواندن Packageهای آزمایشی قبلی باقی مانده‌اند. Validator آن‌ها را Warning می‌کند و Content جدید باید به‌ترتیب `EXERCISE` و `PROJECT_LINK` تولید کند.

## Flashcard

Flashcard یک Resource مستقل Course است و UI، Search، زمان‌بندی مرور، Persistence و Backup آن در Core قرار دارد. Course فقط داده کارت را تعریف می‌کند:

```json
{
  "id": "js-flash-fnd-001",
  "courseId": "javascript",
  "lessonId": "js-fnd-001",
  "front": "Closure چیست؟",
  "back": "تابعی همراه با محیط lexical قابل دسترسی آن.",
  "hint": "به Scope محل تعریف تابع فکر کنید.",
  "tags": ["function", "scope"],
  "difficulty": "MEDIUM"
}
```

قواعد Flashcard:

- `id` پایدار است؛ تغییر آن History مرور را به کارت جدید تبدیل می‌کند.
- `courseId` باید با Manifest یکسان باشد.
- `lessonId` باید به Lesson واقعی همان Package اشاره کند.
- `front` و `back` خالی مجاز نیستند.
- `difficulty` یکی از `EASY`, `MEDIUM`, `HARD` است.
- `hint` و `tags` اختیاری‌اند.
- Course نباید Spaced Repetition، Room table یا UI Deck اختصاصی بسازد؛ همه این بخش‌ها از Core مصرف می‌شوند.

`CoursePackageImporter` کارت‌های جدید را با State اولیه و بدون overwrite کردن Review History قبلی Seed می‌کند. Search Index نیز Front، Back، Hint و Tagهای کارت را ایندکس می‌کند.

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

خطاها Release را متوقف می‌کنند؛ Warning مانند Capability فعال با لیست خالی یا استفاده از alias قدیمی باید پیش از انتشار بررسی شود، ولی برای Draft مانع Compile نیست.