# شرح ماژول‌های Foundation 1.5

## `course`

لایه JVM برای مدل‌ها و قرارداد Serializable محتوای Course است. وابستگی Android ندارد و تغییر ناسازگار آن باید همراه افزایش Course Schema و migration strategy باشد.

## `engine`

منطق قطعی و Unit-testable را نگه می‌دارد: Codec، Validator، SemVer، update decisions، Progress، Learning Path، Quiz، Exercise، Project/Achievement، Search، Review، Placement، Backup contract و Code Runner registry. این ماژول نباید `Context`، Room یا Compose را import کند.

## `core`

Android Runtime و مالک implementation مشترک است:

- `AcademyRuntime` composition root
- Room/DAO/Migration
- Repositoryها و mapping به public modelها
- DataStore و WorkManager
- notification scheduling
- backup/restore
- content loading/update/install/rollback
- backend boundary (`AcademyBackend`)

`core` دیگر مالک Compose presentation، Theme، navigation UI یا App Shell نیست. این ownership به `AS-Academy-MainUi` منتقل شده است.

## `tools`

CLI رسمی Authoring و CI برای Course Package است. `validate` پوشه Course را بررسی می‌کند و `compile` فقط پس از validation خروجی معتبر تولید می‌کند.

## Reference application

ماژول قدیمی `sample-app` از Core بازنشسته شده است. Reference app رسمی در Repository `AS-Academy-MainUi` و ماژول `academy-viewer` قرار دارد تا presentation در مالک صحیح خودش تست شود.

Foundation Integration برنامه مرجع را با هر سه منبع واقعی assemble می‌کند:

```text
Core candidate/main
MainUi main
MainCourse main
      |
      v
academy-viewer assembleDebug
```

## قواعد وابستگی

- `course` به هیچ implementation layer وابسته نیست.
- `engine` فقط منطق JVM و مدل‌های `course` را consume می‌کند.
- `core` از `engine` و `course` استفاده می‌کند و runtime implementation را مالک است.
- `tools` فقط لایه JVM را consume می‌کند.
- MainUi فقط public APIهای Core را consume می‌کند.
- MainCourse runtime implementation ندارد.
- Course App باید thin host باقی بماند و Core/MainUi/MainCourse را fork نکند.
