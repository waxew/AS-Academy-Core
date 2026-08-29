package com.asdevelopers.academy.core.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.asdevelopers.academy.core.settings.AcademyProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** گزینه اختصاصی هر Course بدون تغییر کد Drawer به Core تزریق می‌شود. */
data class AcademyDrawerItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val selected: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Drawer استاندارد AS Academy از سمت راست باز می‌شود و Profile/Settings/Share/About را یکسان نگه می‌دارد.
 */
@Composable
fun AcademyDrawerLayout(
    drawerState: DrawerState,
    profile: AcademyProfile,
    courseItems: List<AcademyDrawerItem>,
    onProfileImageClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onShareClick: () -> Unit,
    onAboutClick: () -> Unit,
    contentIsRtl: Boolean = true,
    content: @Composable () -> Unit
) {
    // Drawer در LayoutDirection.Rtl از سمت راست صفحه باز می‌شود.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(320.dp).fillMaxHeight()) {
                    DrawerProfileHeader(profile, onProfileImageClick)
                    Divider()

                    // طبق قرارداد مشترک، Settings و Share همیشه اولین گزینه‌های عمومی هستند.
                    NavigationDrawerItem(
                        label = { Text("تنظیمات") },
                        selected = false,
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        onClick = onSettingsClick,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("اشتراک‌گذاری با دوستان") },
                        selected = false,
                        icon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                        onClick = onShareClick,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // این ناحیه فقط گزینه‌های اختصاصی Course را دریافت می‌کند.
                    if (courseItems.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        courseItems.forEach { item ->
                            NavigationDrawerItem(
                                label = { Text(item.label) },
                                selected = item.selected,
                                icon = { Icon(item.icon, contentDescription = null) },
                                onClick = item.onClick,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Divider()
                    NavigationDrawerItem(
                        label = { Text("درباره نرم‌افزار") },
                        selected = false,
                        icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                        onClick = onAboutClick,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        ) {
            // محتوای انگلیسی می‌تواند جهت مستقل از Drawer داشته باشد.
            val direction = if (contentIsRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction, content = content)
        }
    }
}

@Composable
private fun DrawerProfileHeader(profile: AcademyProfile, onProfileImageClick: () -> Unit) {
    val context = LocalContext.current
    val profileBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = profile.imageUri
    ) {
        // Decode فایل انتخابی روی IO انجام می‌شود و URI خراب فقط fallback آیکون را نشان می‌دهد.
        value = withContext(Dispatchers.IO) {
            profile.imageUri?.let { rawUri ->
                runCatching {
                    context.decodeProfileBitmap(Uri.parse(rawUri))
                }.getOrNull()
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Slot تصویر واقعی در نسخه میزبان با URI ذخیره‌شده جایگزین می‌شود؛ fallback همیشه قابل لمس است.
        Box(
            modifier = Modifier
                .size(92.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .clickable(onClick = onProfileImageClick),
            contentAlignment = Alignment.Center
        ) {
            if (profileBitmap == null) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "انتخاب تصویر پروفایل",
                    modifier = Modifier.size(46.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Image(
                    bitmap = requireNotNull(profileBitmap),
                    contentDescription = "تصویر پروفایل",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(CircleShape)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(profile.displayName, fontWeight = FontWeight.Bold)
        }
        // URI فقط در Repository نگهداری می‌شود و برای حفظ حریم خصوصی در متن UI نمایش داده نمی‌شود.
        Text("برای تغییر تصویر لمس کنید", style = MaterialTheme.typography.bodySmall)
    }
}

/** تصویر بزرگ انتخابی قبل از ورود به حافظه نزدیک اندازه Header نمونه‌برداری می‌شود. */
private fun Context.decodeProfileBitmap(uri: Uri): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > PROFILE_DECODE_EDGE_PX) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
    }
}

private const val PROFILE_DECODE_EDGE_PX = 512
