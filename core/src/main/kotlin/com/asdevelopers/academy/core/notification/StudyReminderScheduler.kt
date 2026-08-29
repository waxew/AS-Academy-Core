package com.asdevelopers.academy.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Scheduler واحد اعلان مطالعه؛ صفحه Settings فقط این API را صدا می‌زند. */
class StudyReminderScheduler(private val context: Context) {
    /** Host پیش از فعال‌کردن Reminder می‌تواند وضعیت مجوز نسخه‌های جدید Android را بررسی کند. */
    fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun scheduleEvery(days: Long, title: String, message: String) {
        // WorkManager دوره کمتر از یک روز برای Reminder آموزشی ایجاد نمی‌کند تا مزاحمت کاهش یابد.
        require(days >= 1) { "Reminder interval must be at least one day" }
        val input = Data.Builder()
            .putString(StudyReminderWorker.KEY_TITLE, title)
            .putString(StudyReminderWorker.KEY_MESSAGE, message)
            .build()
        val request = PeriodicWorkRequestBuilder<StudyReminderWorker>(days, TimeUnit.DAYS)
            .setInputData(input)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "as_academy_study_reminder"
    }
}

/** Worker فقط اعلان عمومی را نمایش می‌دهد و هیچ منطق Course اختصاصی ندارد. */
class StudyReminderWorker(
    appContext: Context,
    parameters: WorkerParameters
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        // خاموش بودن اعلان‌های سیستم بدون خطا Result.success برمی‌گرداند.
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return Result.success()
        // Work ممکن است قبل از پاسخ کاربر اجرا شود؛ نبود مجوز نباید Worker را Crash یا Retry-loop کند.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()
        createChannelIfRequired()
        val title = inputData.getString(KEY_TITLE) ?: "AS Academy"
        val message = inputData.getString(KEY_MESSAGE) ?: "زمان ادامه مسیر یادگیری است."
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    private fun createChannelIfRequired() {
        // NotificationChannel فقط از Android 8 به بعد وجود دارد.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "یادآور مطالعه", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "یادآوری ادامه دوره‌های AS Academy"
            }
        )
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        private const val CHANNEL_ID = "as_academy_study"
        private const val NOTIFICATION_ID = 41001
    }
}
