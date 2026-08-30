package com.asdevelopers.academy.sample

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import com.asdevelopers.academy.core.database.AcademyDatabase
import com.asdevelopers.academy.core.navigation.AcademyNavHost
import com.asdevelopers.academy.core.navigation.AcademyRoutes
import com.asdevelopers.academy.core.navigation.openAbout
import com.asdevelopers.academy.core.navigation.openFlashcardReview
import com.asdevelopers.academy.core.navigation.openLesson
import com.asdevelopers.academy.core.navigation.openPlacement
import com.asdevelopers.academy.core.navigation.openQuiz
import com.asdevelopers.academy.core.navigation.openSettings
import com.asdevelopers.academy.core.navigation.openWeakTopicReview
import com.asdevelopers.academy.core.notification.StudyReminderScheduler
import com.asdevelopers.academy.core.repository.FlashcardReviewRepository
import com.asdevelopers.academy.core.repository.PlacementResultRepository
import com.asdevelopers.academy.core.repository.QuizHistoryRepository
import com.asdevelopers.academy.core.repository.WeakTopicReviewRepository
import com.asdevelopers.academy.core.review.Flashcard
import com.asdevelopers.academy.core.review.PlacementBand
import com.asdevelopers.academy.core.review.PlacementPolicy
import com.asdevelopers.academy.core.review.SpacedReviewEngine
import com.asdevelopers.academy.core.settings.AcademyPreferencesRepository
import com.asdevelopers.academy.core.settings.AcademyProfile
import com.asdevelopers.academy.core.settings.AcademySettings
import com.asdevelopers.academy.core.settings.AcademyThemeMode
import com.asdevelopers.academy.core.ui.components.AcademyAppShell
import com.asdevelopers.academy.core.ui.components.AcademyDrawerItem
import com.asdevelopers.academy.core.ui.content.LessonRenderer
import com.asdevelopers.academy.core.ui.screens.AcademyAboutScreen
import com.asdevelopers.academy.core.ui.screens.AcademyFlashcardReviewScreen
import com.asdevelopers.academy.core.ui.screens.AcademyPlacementSummaryScreen
import com.asdevelopers.academy.core.ui.screens.AcademyQuizScreen
import com.asdevelopers.academy.core.ui.screens.AcademySettingsScreen
import com.asdevelopers.academy.core.ui.screens.AcademyWeakTopicReviewScreen
import com.asdevelopers.academy.core.ui.theme.AcademyTheme
import com.asdevelopers.academy.core.ui.theme.DefaultAcademyBranding
import com.asdevelopers.academy.core.version.CoreVersion
import com.asdevelopers.academy.course.model.CourseLevelType
import kotlinx.coroutines.launch

/**
 * برنامه نمونه ثابت می‌کند Course فقط محتوا و Branding می‌دهد و workflowهای عمومی از Core می‌آیند.
 * Placement، Weak Topic Review و Flashcard Review نیز عمداً اینجا end-to-end مصرف می‌شوند تا CI integration آن‌ها را بسازد.
 */
@Composable
fun SampleAcademyApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val preferences = remember { AcademyPreferencesRepository(context) }
    val reminderScheduler = remember { StudyReminderScheduler(context) }
    // دیتابیس نمونه همان Persistence واقعی Core را استفاده می‌کند تا Review فقط یک Preview بدون ذخیره نباشد.
    val database = remember { AcademyDatabase.create(context, "sample_academy.db") }
    val quizHistoryRepository = remember(database) { QuizHistoryRepository(database.quizResultDao()) }
    val weakTopicReviewRepository = remember(database) { WeakTopicReviewRepository(database.quizResultDao()) }
    val placementResultRepository = remember(database) { PlacementResultRepository(database.quizResultDao()) }
    val flashcardReviewRepository = remember(database) { FlashcardReviewRepository(database.flashcardProgressDao()) }

    // نمونه فقط یک Level دارد، بنابراین Policy کوچک خودش را تزریق می‌کند و Engine عمومی تغییر نمی‌کند.
    val samplePlacementPolicy = remember {
        PlacementPolicy(
            listOf(
                PlacementBand(
                    minimumScorePercent = 0,
                    levelType = CourseLevelType.FUNDAMENTALS,
                    title = "مبانی نمونه",
                    reviewWeakTopics = true
                )
            )
        )
    }

    DisposableEffect(database) {
        onDispose { database.close() }
    }

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
        },
        AcademyDrawerItem("quiz", "آزمون و تعیین سطح", Icons.Outlined.MenuBook) {
            bundle?.quizzes?.firstOrNull()?.let { navController.openQuiz(it.id) }
        },
        AcademyDrawerItem("weak-review", "مرور نقاط ضعف", Icons.Outlined.MenuBook) {
            navController.openWeakTopicReview()
        },
        AcademyDrawerItem("flashcards", "مرور فلش‌کارت", Icons.Outlined.MenuBook) {
            navController.openFlashcardReview()
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
                            onOpenQuiz = {
                                bundle?.quizzes?.firstOrNull()?.let { navController.openQuiz(it.id) }
                            },
                            onOpenWeakReview = { navController.openWeakTopicReview() },
                            onOpenFlashcards = { navController.openFlashcardReview() },
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
                            versionName = CoreVersion.CURRENT,
                            modifier = Modifier.padding(padding)
                        )
                    },
                    lesson = { lessonId ->
                        val lesson = bundle?.lessons?.firstOrNull { it.id == lessonId }
                        if (lesson == null) {
                            SampleMessage("درس پیدا نشد.", Modifier.padding(padding))
                        } else {
                            LessonRenderer(
                                lesson = lesson,
                                onQuizClick = { navController.openQuiz(it) },
                                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                            )
                        }
                    },
                    quiz = { quizId ->
                        val quiz = bundle?.quizzes?.firstOrNull { it.id == quizId }
                        if (quiz == null) {
                            SampleMessage("آزمون پیدا نشد.", Modifier.padding(padding))
                        } else {
                            AcademyQuizScreen(
                                quiz = quiz,
                                modifier = Modifier.padding(padding),
                                onCompleted = { score ->
                                    scope.launch {
                                        quizHistoryRepository.record(quiz, score, System.currentTimeMillis())
                                        // Quiz نمونه نقش Placement demo را دارد؛ نتیجه از Room در Route بعدی دوباره خوانده می‌شود.
                                        navController.openPlacement()
                                    }
                                }
                            )
                        }
                    },
                    placement = {
                        val placementFlow = remember {
                            placementResultRepository.observeLatest(
                                courseId = "sample",
                                placementQuizId = "sample-quiz-001",
                                policy = samplePlacementPolicy
                            )
                        }
                        val placementState by placementFlow.collectAsState(initial = null)
                        val state = placementState
                        if (state == null) {
                            SampleMessage("ابتدا آزمون نمونه را کامل کنید.", Modifier.padding(padding))
                        } else {
                            AcademyPlacementSummaryScreen(
                                recommendation = state.recommendation,
                                weakTags = state.weakTags,
                                onStartLevel = {
                                    navController.navigate(AcademyRoutes.HOME) { launchSingleTop = true }
                                },
                                onReviewWeakTopics = { navController.openWeakTopicReview() },
                                modifier = Modifier.padding(padding)
                            )
                        }
                    },
                    weakTopicReview = {
                        val currentBundle = bundle
                        if (currentBundle == null) {
                            SampleMessage("Course هنوز بارگذاری نشده است.", Modifier.padding(padding))
                        } else {
                            val recommendationsFlow = remember(currentBundle) {
                                weakTopicReviewRepository.observeRecommendations(currentBundle)
                            }
                            val recommendations by recommendationsFlow.collectAsState(initial = emptyList())
                            AcademyWeakTopicReviewScreen(
                                recommendations = recommendations,
                                onLessonClick = { navController.openLesson(it) },
                                modifier = Modifier.padding(padding)
                            )
                        }
                    },
                    flashcardReview = {
                        val currentBundle = bundle
                        if (currentBundle == null) {
                            SampleMessage("Course هنوز بارگذاری نشده است.", Modifier.padding(padding))
                        } else {
                            val sessionStartedAt = remember { System.currentTimeMillis() }
                            val reviewDay = remember(sessionStartedAt) {
                                SpacedReviewEngine.utcEpochDay(sessionStartedAt)
                            }
                            var cards by remember(currentBundle.manifest.version, reviewDay) {
                                mutableStateOf<List<Flashcard>?>(null)
                            }
                            LaunchedEffect(currentBundle, reviewDay) {
                                cards = flashcardReviewRepository.loadDueCards(currentBundle, reviewDay)
                            }

                            val sessionCards = cards
                            if (sessionCards == null) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(padding),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                AcademyFlashcardReviewScreen(
                                    cards = sessionCards,
                                    onRated = { card, rating ->
                                        scope.launch {
                                            flashcardReviewRepository.recordReview(
                                                courseId = currentBundle.manifest.courseId,
                                                cardId = card.id,
                                                rating = rating,
                                                reviewedEpochDay = reviewDay,
                                                updatedAtEpochMillis = System.currentTimeMillis()
                                            )
                                        }
                                    },
                                    onSessionFinished = { navController.popBackStack() },
                                    modifier = Modifier.padding(padding)
                                )
                            }
                        }
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
    onOpenQuiz: () -> Unit,
    onOpenWeakReview: () -> Unit,
    onOpenFlashcards: () -> Unit,
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
            Button(onClick = onOpenQuiz) { Text("آزمون + نتیجه تعیین سطح") }
            Button(onClick = onOpenWeakReview) { Text("مرور نقاط ضعف") }
            Button(onClick = onOpenFlashcards) { Text("مرور فلش‌کارت") }
        }
    }
}

@Composable
private fun SampleMessage(message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(message)
    }
}
