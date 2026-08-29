package com.asdevelopers.academy.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.asdevelopers.academy.core.code.CodeRunner
import com.asdevelopers.academy.core.content.AssetCoursePackageLoader
import com.asdevelopers.academy.core.database.AcademyDatabase
import com.asdevelopers.academy.core.database.BookmarkEntity
import com.asdevelopers.academy.core.database.LessonProgressEntity
import com.asdevelopers.academy.core.search.SearchIndexer
import com.asdevelopers.academy.core.settings.AcademySettingsRepository
import com.asdevelopers.academy.course.model.CoursePackage
import kotlinx.coroutines.launch

/** App Shell مشترک تمام اپ‌های تک‌دوره‌ای AS Academy. */
@Composable
fun AcademyCourseApp(courseId: String, codeRunner: CodeRunner? = null) {
    val context = LocalContext.current
    val settings = remember { AcademySettingsRepository(context.applicationContext) }
    val darkMode by settings.darkMode.collectAsState(initial = false)

    MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
        var course by remember { mutableStateOf<CoursePackage?>(null) }
        var loadError by remember { mutableStateOf<String?>(null) }
        val db = remember(courseId) { AcademyDatabase.create(context, "as_academy_${courseId}.db") }

        DisposableEffect(db) { onDispose { db.close() } }
        LaunchedEffect(courseId) {
            runCatching { AssetCoursePackageLoader(context.assets).load(courseId) }
                .onSuccess {
                    course = it
                    SearchIndexer(db.searchDao()).rebuild(it)
                }
                .onFailure { loadError = it.message ?: it.toString() }
        }

        when {
            loadError != null -> MessageScreen("خطا در بارگذاری دوره", loadError.orEmpty())
            course == null -> LoadingScreen()
            else -> AcademyShell(requireNotNull(course), db, settings, codeRunner)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcademyShell(
    course: CoursePackage,
    db: AcademyDatabase,
    settings: AcademySettingsRepository,
    codeRunner: CodeRunner?
) {
    val nav = rememberNavController()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AS Academy", style = MaterialTheme.typography.headlineSmall)
                    Text(course.manifest.titleFa, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DrawerItem("خانه", AcademyRoutes.HOME, nav) { scope.launch { drawer.close() } }
                    DrawerItem("جستجو", AcademyRoutes.SEARCH, nav) { scope.launch { drawer.close() } }
                    DrawerItem("علاقه‌مندی‌ها", AcademyRoutes.BOOKMARKS, nav) { scope.launch { drawer.close() } }
                    DrawerItem("پیشرفت", AcademyRoutes.PROGRESS, nav) { scope.launch { drawer.close() } }
                    DrawerItem("تنظیمات", AcademyRoutes.SETTINGS, nav) { scope.launch { drawer.close() } }
                    DrawerItem("درباره نرم‌افزار", AcademyRoutes.ABOUT, nav) { scope.launch { drawer.close() } }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(course.manifest.titleEn) },
                    navigationIcon = {
                        Button(onClick = { scope.launch { drawer.open() } }) { Text("☰") }
                    }
                )
            }
        ) { padding ->
            NavHost(navController = nav, startDestination = AcademyRoutes.HOME, modifier = Modifier.padding(padding)) {
                composable(AcademyRoutes.HOME) { HomeScreen(course, nav) }
                composable(AcademyRoutes.CHAPTERS) { back ->
                    ChapterScreen(course, back.arguments?.getString("levelId").orEmpty(), nav)
                }
                composable(AcademyRoutes.LESSONS) { back ->
                    LessonListScreen(course, back.arguments?.getString("chapterId").orEmpty(), nav)
                }
                composable(AcademyRoutes.LESSON) { back ->
                    LessonScreen(course, back.arguments?.getString("lessonId").orEmpty(), db, codeRunner)
                }
                composable(AcademyRoutes.SEARCH) { SearchScreen(db, nav) }
                composable(AcademyRoutes.BOOKMARKS) { BookmarkScreen(course, db, nav) }
                composable(AcademyRoutes.PROGRESS) { ProgressScreen(course, db) }
                composable(AcademyRoutes.SETTINGS) { SettingsScreen(settings) }
                composable(AcademyRoutes.ABOUT) { AboutScreen(course) }
            }
        }
    }
}

@Composable
private fun DrawerItem(label: String, route: String, nav: NavHostController, close: () -> Unit) {
    NavigationDrawerItem(label = { Text(label) }, selected = false, onClick = { nav.navigate(route); close() })
}

@Composable
private fun HomeScreen(course: CoursePackage, nav: NavHostController) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(course.manifest.titleFa, style = MaterialTheme.typography.headlineMedium)
            Text("از مبانی تا پروژه‌های واقعی", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(course.levels.sortedBy { it.order }, key = { it.id }) { level ->
            val chapters = course.chaptersFor(level.id)
            val count = chapters.sumOf { course.lessonsFor(it.id).size }
            AcademyCard(level.title, "$count درس", onClick = { nav.navigate(AcademyRoutes.chapters(level.id)) })
        }
    }
}

@Composable
private fun ChapterScreen(course: CoursePackage, levelId: String, nav: NavHostController) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(course.chaptersFor(levelId), key = { it.id }) { chapter ->
            AcademyCard(chapter.title, chapter.description, onClick = { nav.navigate(AcademyRoutes.lessons(chapter.id)) })
        }
    }
}

@Composable
private fun LessonListScreen(course: CoursePackage, chapterId: String, nav: NavHostController) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(course.lessonsFor(chapterId), key = { it.id }) { lesson ->
            AcademyCard(lesson.title, "${lesson.estimatedMinutes} دقیقه", onClick = { nav.navigate(AcademyRoutes.lesson(lesson.id)) })
        }
    }
}

@Composable
private fun LessonScreen(course: CoursePackage, lessonId: String, db: AcademyDatabase, runner: CodeRunner?) {
    val lesson = course.lesson(lessonId) ?: return MessageScreen("درس پیدا نشد", lessonId)
    val scope = rememberCoroutineScope()
    var runOutput by remember { mutableStateOf<String?>(null) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(lesson.title, style = MaterialTheme.typography.headlineMedium) }
        item { Text(lesson.summary, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(lesson.blocks, key = { it.id }) { block ->
            LessonBlockView(
                block,
                onRunCode = if (block.type.name == "CODE" && runner != null) { code ->
                    scope.launch {
                        val result = runner.run(code)
                        runOutput = if (result.success) result.output else result.error
                    }
                } else null
            )
        }
        runOutput?.let { output -> item { Text("خروجی:\n$output") } }
        item {
            Button(
                onClick = {
                    scope.launch {
                        db.progressDao().upsert(
                            LessonProgressEntity(
                                lessonId = lessonId,
                                progressPercent = 100,
                                lastBlockIndex = lesson.blocks.lastIndex.coerceAtLeast(0),
                                studySeconds = 0,
                                completed = true,
                                lastOpenedAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("علامت‌گذاری به‌عنوان مطالعه‌شده") }
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        db.bookmarkDao().upsert(
                            BookmarkEntity(
                                id = "lesson:$lessonId",
                                targetType = "LESSON",
                                targetId = lessonId,
                                lessonId = lessonId,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("افزودن به علاقه‌مندی‌ها") }
        }
    }
}

@Composable
private fun ProgressScreen(course: CoursePackage, db: AcademyDatabase) {
    val progress by db.progressDao().observeAll().collectAsState(initial = emptyList())
    val completed = progress.count { it.completed }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("پیشرفت", style = MaterialTheme.typography.headlineMedium)
        Text("$completed از ${course.lessons.size} درس تکمیل شده است")
    }
}

@Composable
private fun BookmarkScreen(course: CoursePackage, db: AcademyDatabase, nav: NavHostController) {
    val bookmarks by db.bookmarkDao().observeAll().collectAsState(initial = emptyList())
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(bookmarks, key = { it.id }) { bookmark ->
            val title = course.lesson(bookmark.targetId)?.title ?: bookmark.targetId
            AcademyCard(title, bookmark.targetType, onClick = {
                if (bookmark.targetType == "LESSON") nav.navigate(AcademyRoutes.lesson(bookmark.targetId))
            })
        }
    }
}

@Composable
private fun SearchScreen(db: AcademyDatabase, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<com.asdevelopers.academy.core.database.SearchIndexEntity>()) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        androidx.compose.material3.OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("جستجو") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            scope.launch {
                results = if (query.isBlank()) emptyList() else db.searchDao().search("${query.trim()}*")
            }
        }) { Text("جستجو") }
        results.forEach { result ->
            AcademyCard(result.title, result.refType, onClick = {
                if (result.refType == "LESSON") nav.navigate(AcademyRoutes.lesson(result.refId))
            })
        }
    }
}

@Composable
private fun SettingsScreen(settings: AcademySettingsRepository) {
    val dark by settings.darkMode.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = { scope.launch { settings.setDarkMode(!dark) } }) {
            Text(if (dark) "حالت روشن" else "حالت تاریک")
        }
    }
}

@Composable
private fun AboutScreen(course: CoursePackage) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(course.manifest.titleFa, style = MaterialTheme.typography.headlineMedium)
        Text("برنامه آموزشی آفلاین AS Academy همراه با درس، تمرین، آزمون و پروژه‌های عملی.")
        Text("نسخه محتوا: ${course.manifest.version}")
        Text("Develop by AS Team Group")
    }
}

@Composable
private fun LoadingScreen() {
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        CircularProgressIndicator()
        Text("در حال بارگذاری دوره...")
    }
}

@Composable
private fun MessageScreen(title: String, message: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(message)
    }
}
