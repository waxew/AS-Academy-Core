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
import com.asdevelopers.academy.core.content.LearningExtras
import com.asdevelopers.academy.core.content.LearningExtrasLoader
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
        var extras by remember { mutableStateOf(LearningExtras()) }
        var loadError by remember { mutableStateOf<String?>(null) }
        val db = remember(courseId) { AcademyDatabase.create(context, "as_academy_${courseId}.db") }
        DisposableEffect(db) { onDispose { db.close() } }
        LaunchedEffect(courseId) {
            runCatching { AssetCoursePackageLoader(context.assets).load(courseId) to LearningExtrasLoader(context.assets).load(courseId) }
                .onSuccess { (loadedCourse, loadedExtras) -> course = loadedCourse; extras = loadedExtras; SearchIndexer(db.searchDao()).rebuild(loadedCourse) }
                .onFailure { loadError = it.message ?: it.toString() }
        }
        when { loadError != null -> MessageScreen("خطا در بارگذاری دوره", loadError.orEmpty()); course == null -> LoadingScreen(); else -> AcademyShell(requireNotNull(course), extras, db, settings, codeRunner) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcademyShell(course: CoursePackage, extras: LearningExtras, db: AcademyDatabase, settings: AcademySettingsRepository, codeRunner: CodeRunner?) {
    val nav = rememberNavController(); val drawer = rememberDrawerState(DrawerValue.Closed); val scope = rememberCoroutineScope()
    ModalNavigationDrawer(drawerState = drawer, drawerContent = { ModalDrawerSheet { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("AS Academy", style = MaterialTheme.typography.headlineSmall); Text(course.manifest.titleFa, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DrawerItem("خانه", AcademyRoutes.HOME, nav) { scope.launch { drawer.close() } }; DrawerItem("تمرین‌ها", AcademyRoutes.EXERCISES, nav) { scope.launch { drawer.close() } }; DrawerItem("آزمون‌ها", AcademyRoutes.QUIZZES, nav) { scope.launch { drawer.close() } }; DrawerItem("پروژه‌ها", AcademyRoutes.PROJECTS, nav) { scope.launch { drawer.close() } }; DrawerItem("واژه‌نامه", AcademyRoutes.GLOSSARY, nav) { scope.launch { drawer.close() } }; DrawerItem("جستجو", AcademyRoutes.SEARCH, nav) { scope.launch { drawer.close() } }; DrawerItem("علاقه‌مندی‌ها", AcademyRoutes.BOOKMARKS, nav) { scope.launch { drawer.close() } }; DrawerItem("پیشرفت", AcademyRoutes.PROGRESS, nav) { scope.launch { drawer.close() } }; DrawerItem("تنظیمات", AcademyRoutes.SETTINGS, nav) { scope.launch { drawer.close() } }; DrawerItem("درباره نرم‌افزار", AcademyRoutes.ABOUT, nav) { scope.launch { drawer.close() } }
    } } }) {
        Scaffold(topBar = { TopAppBar(title = { Text(course.manifest.titleEn) }, navigationIcon = { Button(onClick = { scope.launch { drawer.open() } }) { Text("☰") } }) }) { padding ->
            NavHost(nav, AcademyRoutes.HOME, Modifier.padding(padding)) {
                composable(AcademyRoutes.HOME) { HomeScreen(course, extras, nav) }; composable(AcademyRoutes.CHAPTERS) { ChapterScreen(course, it.arguments?.getString("levelId").orEmpty(), nav) }; composable(AcademyRoutes.LESSONS) { LessonListScreen(course, it.arguments?.getString("chapterId").orEmpty(), nav) }; composable(AcademyRoutes.LESSON) { LessonScreen(course, extras, it.arguments?.getString("lessonId").orEmpty(), db, codeRunner, nav) }
                composable(AcademyRoutes.EXERCISES) { ExerciseListScreen(extras, nav) }; composable(AcademyRoutes.EXERCISE) { ExerciseDetailScreen(extras, it.arguments?.getString("exerciseId").orEmpty()) }; composable(AcademyRoutes.QUIZZES) { QuizListScreen(extras, nav) }; composable(AcademyRoutes.QUIZ) { QuizDetailScreen(extras, it.arguments?.getString("quizId").orEmpty(), db) }; composable(AcademyRoutes.PROJECTS) { ProjectListScreen(extras, nav) }; composable(AcademyRoutes.PROJECT) { ProjectDetailScreen(extras, it.arguments?.getString("projectId").orEmpty()) }; composable(AcademyRoutes.GLOSSARY) { GlossaryScreen(extras) }; composable(AcademyRoutes.SEARCH) { SearchScreen(db, nav) }; composable(AcademyRoutes.BOOKMARKS) { BookmarkScreen(course, db, nav) }; composable(AcademyRoutes.PROGRESS) { ProgressScreen(course, db) }; composable(AcademyRoutes.SETTINGS) { SettingsScreen(settings) }; composable(AcademyRoutes.ABOUT) { AboutScreen(course) }
            }
        }
    }
}

@Composable private fun DrawerItem(label: String, route: String, nav: NavHostController, close: () -> Unit) { NavigationDrawerItem(label = { Text(label) }, selected = false, onClick = { nav.navigate(route); close() }) }

@Composable private fun HomeScreen(course: CoursePackage, extras: LearningExtras, nav: NavHostController) { LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item { Text(course.manifest.titleFa, style = MaterialTheme.typography.headlineMedium); Text("از مبانی تا پروژه‌های واقعی", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    item { AcademyCard("تمرین‌ها", "${extras.exercises.size} تمرین عملی", onClick = { nav.navigate(AcademyRoutes.EXERCISES) }) }; item { AcademyCard("آزمون‌ها", "${extras.quizzes.sumOf { it.questions.size }} سؤال", onClick = { nav.navigate(AcademyRoutes.QUIZZES) }) }; item { AcademyCard("پروژه‌های عملی", "${extras.projects.size} پروژه", onClick = { nav.navigate(AcademyRoutes.PROJECTS) }) }; item { AcademyCard("واژه‌نامه", "${extras.glossary.size} اصطلاح", onClick = { nav.navigate(AcademyRoutes.GLOSSARY) }) }
    items(course.levels.sortedBy { it.order }, key = { it.id }) { level -> val chapters = course.chaptersFor(level.id); AcademyCard(level.title, "${chapters.sumOf { course.lessonsFor(it.id).size }} درس", onClick = { nav.navigate(AcademyRoutes.chapters(level.id)) }) }
} }
@Composable private fun ChapterScreen(course: CoursePackage, levelId: String, nav: NavHostController) { LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(course.chaptersFor(levelId), key = { it.id }) { AcademyCard(it.title, it.description, onClick = { nav.navigate(AcademyRoutes.lessons(it.id)) }) } } }
@Composable private fun LessonListScreen(course: CoursePackage, chapterId: String, nav: NavHostController) { LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(course.lessonsFor(chapterId), key = { it.id }) { AcademyCard(it.title, "${it.estimatedMinutes} دقیقه", onClick = { nav.navigate(AcademyRoutes.lesson(it.id)) }) } } }

@Composable private fun LessonScreen(course: CoursePackage, extras: LearningExtras, lessonId: String, db: AcademyDatabase, runner: CodeRunner?, nav: NavHostController) {
    val lesson = course.lesson(lessonId) ?: return MessageScreen("درس پیدا نشد", lessonId); val scope = rememberCoroutineScope(); var runOutput by remember { mutableStateOf<String?>(null) }; val relatedExercises = extras.exercises.filter { it.lessonId == lessonId }; val relatedQuizzes = extras.quizzes.filter { it.lessonId == lessonId }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(lesson.title, style = MaterialTheme.typography.headlineMedium) }; item { Text(lesson.summary, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(lesson.blocks, key = { it.id }) { block -> LessonBlockView(block, onRunCode = if (block.type.name == "CODE" && runner != null) { code -> scope.launch { val result = runner.run(code); runOutput = if (result.success) result.output else result.error } } else null) }
        runOutput?.let { output -> item { Text("خروجی:\n$output") } }; items(relatedExercises, key = { it.id }) { exercise -> AcademyCard("تمرین: ${exercise.title}", exercise.difficulty.name, onClick = { nav.navigate(AcademyRoutes.exercise(exercise.id)) }) }; items(relatedQuizzes, key = { it.id }) { quiz -> AcademyCard("آزمون: ${quiz.title}", "${quiz.questions.size} سؤال", onClick = { nav.navigate(AcademyRoutes.quiz(quiz.id)) }) }
        item { Button(onClick = { scope.launch { db.progressDao().upsert(LessonProgressEntity(lessonId, 100, lesson.blocks.lastIndex.coerceAtLeast(0), 0, true, System.currentTimeMillis())) } }, modifier = Modifier.fillMaxWidth()) { Text("علامت‌گذاری به‌عنوان مطالعه‌شده") } }
        item { Button(onClick = { scope.launch { db.bookmarkDao().upsert(BookmarkEntity("lesson:$lessonId", "LESSON", lessonId, lessonId, System.currentTimeMillis())) } }, modifier = Modifier.fillMaxWidth()) { Text("افزودن به علاقه‌مندی‌ها") } }
    }
}
@Composable private fun ProgressScreen(course: CoursePackage, db: AcademyDatabase) { val progress by db.progressDao().observeAll().collectAsState(initial = emptyList()); val completed = progress.count { it.completed }; Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("پیشرفت", style = MaterialTheme.typography.headlineMedium); Text("$completed از ${course.lessons.size} درس تکمیل شده است") } }
@Composable private fun BookmarkScreen(course: CoursePackage, db: AcademyDatabase, nav: NavHostController) { val bookmarks by db.bookmarkDao().observeAll().collectAsState(initial = emptyList()); LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(bookmarks, key = { it.id }) { bookmark -> AcademyCard(course.lesson(bookmark.targetId)?.title ?: bookmark.targetId, bookmark.targetType, onClick = { if (bookmark.targetType == "LESSON") nav.navigate(AcademyRoutes.lesson(bookmark.targetId)) }) } } }
@Composable private fun SearchScreen(db: AcademyDatabase, nav: NavHostController) { val scope = rememberCoroutineScope(); var query by remember { mutableStateOf("") }; var results by remember { mutableStateOf(emptyList<com.asdevelopers.academy.core.database.SearchIndexEntity>()) }; Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { androidx.compose.material3.OutlinedTextField(query, { query = it }, label = { Text("جستجو") }, modifier = Modifier.fillMaxWidth()); Button(onClick = { scope.launch { results = if (query.isBlank()) emptyList() else db.searchDao().search("${query.trim()}*") } }) { Text("جستجو") }; results.forEach { result -> AcademyCard(result.title, result.refType, onClick = { if (result.refType == "LESSON") nav.navigate(AcademyRoutes.lesson(result.refId)) }) } } }
@Composable private fun SettingsScreen(settings: AcademySettingsRepository) { val dark by settings.darkMode.collectAsState(initial = false); val scope = rememberCoroutineScope(); Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("تنظیمات", style = MaterialTheme.typography.headlineMedium); Button(onClick = { scope.launch { settings.setDarkMode(!dark) } }) { Text(if (dark) "حالت روشن" else "حالت تاریک") } } }
@Composable private fun AboutScreen(course: CoursePackage) { Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(course.manifest.titleFa, style = MaterialTheme.typography.headlineMedium); Text("برنامه آموزشی آفلاین AS Academy همراه با درس، تمرین، آزمون و پروژه‌های عملی."); Text("نسخه محتوا: ${course.manifest.version}"); Text("Develop by AS Team Group") } }
@Composable private fun LoadingScreen() { Column(Modifier.fillMaxSize().padding(32.dp)) { CircularProgressIndicator(); Text("در حال بارگذاری دوره...") } }
@Composable private fun MessageScreen(title: String, message: String) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, style = MaterialTheme.typography.headlineSmall); Text(message) } }
