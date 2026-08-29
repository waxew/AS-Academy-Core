# شرح ماژول‌ها

## `course`

کوچک‌ترین لایه و منبع رسمی مدل‌های Course است. مدل‌ها با `kotlinx.serialization` تعریف شده‌اند و هیچ وابستگی Android ندارند. تغییر ناسازگار مدل باید همراه افزایش Course Schema و مسیر Migration محتوا باشد.

## `engine`

منطق قطعی و قابل Unit Test را نگه می‌دارد: Codec، Validator، SemVer، Update decision، Progress، Continue Learning، Level unlocking، Quiz scoring، Exercise evaluation، Project/Achievement models، Search document builder، Backup contract و Code Runner registry. این ماژول نباید `Context`، Room یا Compose را import کند.

## `core`

Android Runtime است. Entityها و DAOها، Repositoryها، مهاجرت/اتصال داده Legacy، DataStore، WorkManager، Navigation Compose، App Shell، Theme و Lesson Renderer در این ماژول قرار دارند. `core` APIهای `engine` و `course` را به مصرف‌کننده منتقل می‌کند؛ پس Host معمولاً فقط همین Dependency را لازم دارد.

## `tools`

CLI رسمی Course Authoring است. دستور `validate` یک پوشه را بدون نوشتن خروجی بررسی می‌کند و دستور `compile` پس از Validation یک Bundle واحد می‌سازد. Exit code غیرصفر باعث شکست CI Course Repository می‌شود.

## `sample-app`

قرارداد مصرف را به شکل اجرایی نشان می‌دهد: Asset bundle، Loader، Theme، Drawer، Navigation، Settings، Notification permission و Lesson Renderer. این ماژول منبع کد برای Copy/Paste نیست؛ نمونه استفاده از APIهای `core` است.

## قواعد وابستگی

- `course` به هیچ ماژول داخلی وابسته نیست.
- `engine` فقط به `course` وابسته است.
- `core` به `engine` و `course` وابسته است.
- `tools` فقط منطق JVM را مصرف می‌کند.
- Course app می‌تواند Adapter اختصاصی را به API Core تزریق کند، اما Core نباید به Course app وابسته شود.
