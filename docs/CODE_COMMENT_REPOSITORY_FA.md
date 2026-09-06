# استاندارد کامنت‌گذاری Repository Layer

## هدف

Repository در معماری AS Academy مرز بین لایه داده و لایه مصرف‌کننده است.

UI، ViewModel و Course Application نباید به DAO یا Entityهای دیتابیس وابسته شوند.

## مسئولیت Repository

- تبدیل Entityهای دیتابیس به Modelهای قابل استفاده
- مدیریت عملیات خواندن و نوشتن داده
- کنترل قوانین Domain مربوط به داده
- ایجاد API پایدار برای لایه‌های بالاتر

## مسیر وابستگی صحیح

```
MainUi / MainCourse
        |
        v
Repository Layer
        |
        v
Database DAO
        |
        v
Room Database
```

## قوانین کامنت‌نویسی

هر Repository اصلی باید توضیح دهد:

- مالک چه داده‌ای است
- با چه DAOهایی ارتباط دارد
- چه چیزی را نباید مدیریت کند

## ممنوعیت‌ها

Repository نباید:

- کد UI داشته باشد
- Navigation انجام دهد
- به App خاص وابسته شود
- منطق نمایش محتوا را مدیریت کند

## وضعیت Core

این استاندارد برای تمام Repositoryهای AS Academy اعمال می‌شود.
