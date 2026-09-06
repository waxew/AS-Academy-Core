# راهنمای Runtime در AS Academy Core

## هدف

Runtime قلب اجرای Core است و مسئول آماده‌سازی سرویس‌های مشترک برای تمام برنامه‌های AS Academy می‌باشد.

## مسئولیت‌ها

- مدیریت چرخه عمر سرویس‌های Core
- ایجاد Dependency های مورد نیاز
- اتصال Repository ها و Engine ها
- فراهم کردن محیط اجرای استاندارد برای App ها

## قوانین معماری

Runtime نباید:

- وابسته به UI باشد
- شامل منطق نمایش باشد
- به یک Course خاص وابسته شود

## جریان ارتباط

Application

↓

MainUi

↓

Core Runtime

↓

Repository / Database / Engine

## استاندارد توسعه

هر تغییر در Runtime باید همراه با توضیح معماری و دلیل تغییر ثبت شود.
