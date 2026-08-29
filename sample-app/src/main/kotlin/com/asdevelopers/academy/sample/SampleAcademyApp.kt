package com.asdevelopers.academy.sample

import android.content.Intent
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.asdevelopers.academy.core.content.AssetCoursePackageSource
import com.asdevelopers.academy.core.content.CourseBundle
import com.asdevelopers.academy.core.content.CourseLoadResult
import com.asdevelopers.academy.core.content.CoursePackageLoader
import com.asdevelopers.academy.core.navigation.AcademyNavHost
import com.asdevelopers.academy.core.navigation.AcademyRoutes
import com.asdevelopers.academy.core.navigation.openAbout
import com.asdevelopers.academy.core.navigation.openLesson
import com.asdevelopers.academy.core.navigation.openSettings
import com.asdevelopers.academy.core.notification.StudyReminderScheduler
import com.asdevelopers.academy.core.settings.AcademyPreferencesRepository
import com.asdevelopers.academy.core.settings.AcademyProfile
import com.asdevelopers.academy.core.settings.AcademySettings
import com.asdevelopers.academy.core.settings.AcademyThemeMode
import com.asdevelopers.academy.core.ui.components.AcademyAppShell
import com.asdevelopers.academy.core.ui.components.AcademyDrawerItem
import com.asdevelopers.academy.core.ui.content.LessonRenderer
import com.asdevelopers.academy.core.ui.screens.AcademyAboutScreen
import com.asdevelopers.academy.core.ui.screens.AcademySettingsScreen
import com.asdevelopers.academy.core.ui.theme.AcademyTheme
import com.asdevelopers.academy.core.ui.theme.DefaultAcademyBranding
import kotlinx.coroutines.launch

/** برنامه نمونه ثابت می‌کند Course فقط محتوا و Branding می‌دهد و تمام Screenهای عمومی از Core می‌آیند. */
@Composable
fun SampleAcademyApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val preferences = remember { AcademyPreferencesRepository(context) }
    val reminderScheduler = remember { StudyReminderScheduler(context) }
    val profile by preferences.profile.collectAsState(initial = AcademyProfile())
    val settings by preferences.settings.collectAsState(initial = AcademySettings())
    var courseResult by remember { mutableStateOf<CourseLoadResult?>(null) }

    // Loader همان فایل Offline را از assets می‌خواند که در اپ‌های دوره‌ای قرار می‌گیرد.
    LaunchedEffect(Unit) {
        courseResult = CoursePackageLoader().load(AssetCoursePackageSource(context, "sample-course.json"))
    }

    // انتخاب تصویر توسط Host انجام و URI فقط در DataStore مشترک ذخیره می‌شود.
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // دسترسی خواندن URI برای اجرای بعدی برنامه حفظ می‌شود؛ فقط خود URI در DataStore ذخیره می‌شود.
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            scope.launch { preferences.updateProfile(profile.displayName, uri.toString()) }
        }
    }
    // Android 13 به بعد مجوز اعلان را در زمان اجرا می‌خواهد؛ Courseها همین الگوی Host را تکرار می‌کنند.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) scope.launch { preferences.setNotificationsEnabled(false) }
    }

    val bundle = (courseResult as? CourseLoadResult.Success)?.bundle
    val courseItems = listOf(
        AcademyDrawerItem("home", "خانه", Icons.Outlined.Home, selected = true) {
            navController.navigate(AcademyRoutes.HOME) { launchSingleTop = true; popUpTo(AcademyRoutes.HOME) }
        },
        AcademyDrawerItem("lesson", "درس نمونه", Icons.Outlined.MenuBook) {
            bundle?.lessons?.firstOrNull()?.let { navController.openLesson(it.id) }
        }
    )

    val systemUsesDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = when (settings.themeMode) {
        AcademyThemeMode.SYSTEM -> systemUsesDarkTheme
        AcademyThemeMode.LIGHT -> false
        AcademyThemeMode.DARK -> true
    }
    val deviceDensity = LocalDensity.current

    // Theme، Branding و Font scale ذخیره‌شده واقعاً روی کل Shell اعمال می‌شوند.
    AcademyTheme(branding = bundle?.branding ?: DefaultAcademyBranding, darkTheme = useDarkTheme) {
        CompositionLocalProvider(
            LocalDensity provides Density(deviceDensity.density, settings.fontScale)
        ) {
            AcademyAppShell(
                title = bundle?.manifest?.titleFa ?: "AS Academy Core",
                profile = profile,
                courseItems = courseItems,
                onProfileImageClick = { imagePicker.launch(arrayOf("image/*")) },
                onSettingsClick = { navController.openSettings() },
                onShareClick = {
                    // اشتراک‌گذاری از Intent استاندارد استفاده می‌کند و هیچ SDK اختصاصی به Core تحمیل نمی‌شود.
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "AS Academy — مسیر آموزش از مبانی تا تخصصی")
                    }
                    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
                },
                onAboutClick = { navController.openAbout() },
                contentIsRtl = bundle?.manifest?.rtl ?: true
            ) { padding ->
                AcademyNavHost(
                    navController = navController,
                    home = {
                        SampleHome(
                            result = courseResult,
                            bundle = bundle,
                            onOpenLesson = {
                                bundle?.lessons?.firstOrNull()?.let { navController.openLesson(it.id) }
                            },
                            modifier = Modifier.padding(padding)
                        )
                    },
                    settings = {
                        AcademySettingsScreen(
                            settings = settings,
                            onThemeChanged = { scope.launch { preferences.setThemeMode(it) } },
                            onNotificationsChanged = { enabled ->
                                scope.launch { preferences.setNotificationsEnabled(enabled) }
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !reminderScheduler.canPostNotifications()
                                    ) {
                                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    reminderScheduler.scheduleEvery(1, "AS Academy", "زمان ادامه یادگیری است.")
                                } else {
                                    reminderScheduler.cancel()
                                }
                            },
                            onFontScaleChanged = { scope.launch { preferences.setFontScale(it) } },
                            modifier = Modifier.padding(padding)
                        )
                    },
                    about = {
                        AcademyAboutScreen(
                            appTitle = bundle?.manifest?.titleFa ?: "AS Academy",
                            description = "نمونه اجرایی هسته مرکزی برنامه‌های آموزشی AS Academy.",
                            versionName = "1.0.0",
                            modifier = Modifier.padding(padding)
                        )
                    },
                    lesson = { lessonId ->
                        val lesson = bundle?.lessons?.firstOrNull { it.id == lessonId }
                        if (lesson == null) SampleMessage("درس پیدا نشد.", Modifier.padding(padding))
                        else LessonRenderer(lesson, modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp))
                    }
                )
            }
        }
    }
}

@Composable
private fun SampleHome(
    result: CourseLoadResult?,
    bundle: CourseBundle?,
    onOpenLesson: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (result) {
        null -> Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
        }
        is CourseLoadResult.Failure -> SampleMessage("خطا در خواندن دوره: ${result.message}", modifier)
        is CourseLoadResult.Invalid -> SampleMessage("Course Package نامعتبر است:\n${result.errors.joinToString("\n")}", modifier)
        is CourseLoadResult.Success -> Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(bundle?.manifest?.titleFa.orEmpty(), style = MaterialTheme.typography.headlineMedium)
            Text("Core با موفقیت متصل شده و Course Package اعتبارسنجی شده است.")
            Text("${bundle?.levels?.size ?: 0} سطح • ${bundle?.lessons?.size ?: 0} درس")
            Button(onClick = onOpenLesson) { Text("باز کردن درس نمونه") }
        }
    }
}

@Composable
private fun SampleMessage(message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(message)
    }
}
