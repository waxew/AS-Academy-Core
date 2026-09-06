# راهنمای Repository Layer در AS Academy Core

## هدف

Repository Layer مرز بین منطق برنامه و منابع داده است.

## مسئولیت‌ها

- مدیریت دسترسی به داده‌ها
- هماهنگی بین Database و Application Layer
- جلوگیری از پخش شدن منطق ذخیره‌سازی در بخش‌های دیگر

## قوانین

UI نباید مستقیماً به Database دسترسی داشته باشد.

Course Module نباید جزئیات ذخیره‌سازی را بداند.

Repository مالک قرارداد دسترسی به داده است.

## معماری

UI / Application

↓

Repository

↓

Database / Storage

## استاندارد توسعه

هر Repository جدید باید مسئولیت مشخص، Interface واضح و مستندات فارسی داشته باشد.
