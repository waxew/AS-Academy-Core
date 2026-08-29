package com.asdevelopers.academy.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.asdevelopers.academy.course.model.LessonBlock
import com.asdevelopers.academy.course.model.LessonBlockType

/**
 * کارت عمومی AS Academy.
 * onClick عمداً آخرین پارامتر است تا تمام call siteها بتوانند از trailing lambda استاندارد Kotlin استفاده کنند.
 */
@Composable
fun AcademyCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Renderer عمومی بلوک درس. Blockهای زبان خاص باید از Plugin API توسعه داده شوند. */
@Composable
fun LessonBlockView(
    block: LessonBlock,
    onRunCode: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (block.type) {
        LessonBlockType.TITLE -> Text(block.content, style = MaterialTheme.typography.headlineSmall, modifier = modifier)
        LessonBlockType.SUBTITLE -> Text(block.content, style = MaterialTheme.typography.titleLarge, modifier = modifier)
        LessonBlockType.CODE -> Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(14.dp)
        ) {
            Text(block.content, color = MaterialTheme.colorScheme.inverseOnSurface, fontFamily = FontFamily.Monospace)
            if (onRunCode != null) {
                Text(
                    "اجرا",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp).clickable { onRunCode(block.content) }
                )
            }
        }
        LessonBlockType.TIP -> Callout("نکته", block.content)
        LessonBlockType.WARNING -> Callout("هشدار", block.content)
        LessonBlockType.IMPORTANT -> Callout("مهم", block.content)
        LessonBlockType.NOTE -> Callout("یادداشت", block.content)
        else -> Text(block.content, style = MaterialTheme.typography.bodyLarge, modifier = modifier)
    }
}

@Composable
private fun Callout(label: String, text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$label:", style = MaterialTheme.typography.labelLarge)
            Text(text, modifier = Modifier.weight(1f))
        }
    }
}
