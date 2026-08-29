package com.asdevelopers.academy.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.asdevelopers.academy.core.settings.AcademyProfile
import kotlinx.coroutines.launch

/**
 * App Shell مشترک TopBar، Back behavior سطح صفحه و Drawer راست را برای تمام Courseها یکسان می‌کند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademyAppShell(
    title: String,
    profile: AcademyProfile,
    courseItems: List<AcademyDrawerItem>,
    onProfileImageClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onShareClick: () -> Unit,
    onAboutClick: () -> Unit,
    contentIsRtl: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val closeDrawerThen: (() -> Unit) -> Unit = { action ->
        // ابتدا Drawer بسته می‌شود تا destination جدید زیر لایه باز Drawer پنهان نماند.
        scope.launch {
            drawerState.close()
            action()
        }
    }
    AcademyDrawerLayout(
        drawerState = drawerState,
        profile = profile,
        courseItems = courseItems.map { item -> item.copy(onClick = { closeDrawerThen(item.onClick) }) },
        onProfileImageClick = onProfileImageClick,
        onSettingsClick = { closeDrawerThen(onSettingsClick) },
        onShareClick = { closeDrawerThen(onShareClick) },
        onAboutClick = { closeDrawerThen(onAboutClick) },
        contentIsRtl = contentIsRtl
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        // در RTL آیکون navigation در سمت راست TopBar نمایش داده می‌شود.
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "باز کردن منو")
                        }
                    }
                )
            },
            content = content
        )
    }
}
