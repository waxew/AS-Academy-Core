package com.asdevelopers.academy.core.ui.flashcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asdevelopers.academy.core.flashcard.FlashcardRecallRating
import com.asdevelopers.academy.course.model.Flashcard

/**
 * Deck عمومی Flashcard برای همه Courseها.
 *
 * Course فقط List<Flashcard> می‌دهد؛ منطق Navigation، reveal پاسخ و Rating در Core باقی می‌ماند.
 * ذخیره FlashcardReviewState وظیفه caller است تا Core بتواند Repository/Room مشترک را بدون وابستگی UI نگه دارد.
 */
@Composable
fun FlashcardDeck(
    cards: List<Flashcard>,
    modifier: Modifier = Modifier,
    onReview: (Flashcard, FlashcardRecallRating) -> Unit = { _, _ -> },
    onDeckCompleted: () -> Unit = {}
) {
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var answerVisible by rememberSaveable { mutableStateOf(false) }
    var deckCompleted by rememberSaveable { mutableStateOf(false) }

    if (cards.isEmpty()) {
        Card(modifier = modifier.fillMaxWidth()) {
            Text(
                text = "برای این بخش هنوز Flashcard تعریف نشده است.",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    if (deckCompleted) {
        Card(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "مرور این Deck کامل شد.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "موعد مرور بعدی هر کارت بر اساس Rating شما توسط FlashcardReviewEngine محاسبه می‌شود.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        currentIndex = 0
                        answerVisible = false
                        deckCompleted = false
                    }
                ) {
                    Text("مرور دوباره")
                }
            }
        }
        return
    }

    // اگر Deck در اثر Update Course کوتاه‌تر شود، index قبلی از محدوده خارج نمی‌شود.
    val safeIndex = currentIndex.coerceIn(0, cards.lastIndex)
    val card = cards[safeIndex]
    val progress = (safeIndex + 1).toFloat() / cards.size.toFloat()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Flashcard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${safeIndex + 1} / ${cards.size}", style = MaterialTheme.typography.labelLarge)
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = card.front,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                card.hint?.takeIf(String::isNotBlank)?.let { hint ->
                    Text(
                        text = "راهنما: $hint",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (!answerVisible) {
                    Button(
                        onClick = { answerVisible = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("نمایش پاسخ")
                    }
                } else {
                    HorizontalDivider()
                    Text(
                        text = card.back,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "این پاسخ را چقدر خوب به یاد آوردید؟",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    RecallRatingButtons(
                        onRating = { rating ->
                            onReview(card, rating)
                            answerVisible = false
                            if (safeIndex == cards.lastIndex) {
                                deckCompleted = true
                                onDeckCompleted()
                            } else {
                                currentIndex = safeIndex + 1
                            }
                        }
                    )
                }
            }
        }
    }
}

/** چهار Rating استاندارد موتور مرور را در دو ردیف فشرده برای صفحه موبایل نمایش می‌دهد. */
@Composable
private fun RecallRatingButtons(
    onRating: (FlashcardRecallRating) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onRating(FlashcardRecallRating.AGAIN) },
                modifier = Modifier.weight(1f)
            ) {
                Text("دوباره")
            }
            OutlinedButton(
                onClick = { onRating(FlashcardRecallRating.HARD) },
                modifier = Modifier.weight(1f)
            ) {
                Text("سخت")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onRating(FlashcardRecallRating.GOOD) },
                modifier = Modifier.weight(1f)
            ) {
                Text("خوب")
            }
            OutlinedButton(
                onClick = { onRating(FlashcardRecallRating.EASY) },
                modifier = Modifier.weight(1f)
            ) {
                Text("آسان")
            }
        }
    }
}
