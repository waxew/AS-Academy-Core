package com.asdevelopers.academy.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asdevelopers.academy.core.review.Flashcard
import com.asdevelopers.academy.core.review.ReviewRating

/**
 * Session عمومی Flashcard Review برای تمام Courseها.
 * این UI هیچ وابستگی مستقیمی به Room ندارد؛ Rating از Callback به FlashcardReviewRepository داده می‌شود.
 */
@Composable
fun AcademyFlashcardReviewScreen(
    cards: List<Flashcard>,
    modifier: Modifier = Modifier,
    sessionTitle: String = "مرور فاصله‌دار",
    onRated: (Flashcard, ReviewRating) -> Unit,
    onSessionFinished: () -> Unit = {}
) {
    var currentIndex by remember(cards.map(Flashcard::id)) { mutableStateOf(0) }
    var answerVisible by remember(cards.map(Flashcard::id), currentIndex) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(sessionTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        if (cards.isEmpty()) {
            Text("در حال حاضر کارتی برای مرور وجود ندارد.", style = MaterialTheme.typography.bodyLarge)
            return@Column
        }

        if (currentIndex >= cards.size) {
            LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth())
            Text("مرور این جلسه کامل شد.", style = MaterialTheme.typography.titleLarge)
            Text("زمان مرور بعدی هر کارت بر اساس پاسخ‌های همین جلسه محاسبه می‌شود.")
            Button(onClick = onSessionFinished, modifier = Modifier.fillMaxWidth()) {
                Text("پایان جلسه")
            }
            return@Column
        }

        val card = cards[currentIndex]
        val completed = currentIndex
        LinearProgressIndicator(
            progress = { completed.toFloat() / cards.size.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
        Text("کارت ${currentIndex + 1} از ${cards.size}", style = MaterialTheme.typography.labelLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("سؤال / اصطلاح", style = MaterialTheme.typography.labelLarge)
                Text(card.front, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                if (card.aliases.isNotEmpty()) {
                    Text("نام‌های دیگر: ${card.aliases.joinToString("، ")}")
                }

                if (!answerVisible) {
                    OutlinedButton(
                        onClick = { answerVisible = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("نمایش پاسخ")
                    }
                } else {
                    Text("پاسخ", style = MaterialTheme.typography.labelLarge)
                    Text(card.back, style = MaterialTheme.typography.bodyLarge)

                    card.tags.takeIf(Set<String>::isNotEmpty)?.let { tags ->
                        Text("موضوع‌ها: ${tags.sorted().joinToString("، ")}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (answerVisible) {
            Text("یادآوری این کارت چقدر سخت بود؟", style = MaterialTheme.typography.titleMedium)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onRated(card, ReviewRating.AGAIN)
                        answerVisible = false
                        currentIndex += 1
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("دوباره") }
                OutlinedButton(
                    onClick = {
                        onRated(card, ReviewRating.HARD)
                        answerVisible = false
                        currentIndex += 1
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("سخت") }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onRated(card, ReviewRating.GOOD)
                        answerVisible = false
                        currentIndex += 1
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("خوب") }
                Button(
                    onClick = {
                        onRated(card, ReviewRating.EASY)
                        answerVisible = false
                        currentIndex += 1
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("آسان") }
            }
        }
    }
}
