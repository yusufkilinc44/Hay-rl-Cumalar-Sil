package com.hayirlicumalarsil.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class OnboardPage(
    val emoji: String,
    val title: String,
    val body: String,
    val gradient: List<Color>,
)

private val onboardPages = listOf(
    OnboardPage(
        emoji = "🕌",
        title = "Hayırlı Cumalar Sil'e hoş geldin",
        body = "Her hafta perşembe ve cuma günleri sevdiklerin, Müslümanlar için kutsal " +
            "olan cuma gününü kutlamak için birbirinden güzel \"Hayırlı Cumalar\" görselleri " +
            "gönderir. Güzel bir gelenek 💚",
        gradient = listOf(Color(0xFF6C3DF4), Color(0xFF9C27B0), Color(0xFFE91E63)),
    ),
    OnboardPage(
        emoji = "📥",
        title = "Ama galeri şişiyor",
        body = "Bu görseller her hafta onlarca kişiden gelir ve WhatsApp klasöründe birikir. " +
            "Zamanla telefonun hafızasında ciddi yer kaplar, depolama dolar ve telefon " +
            "yavaşlamaya başlar.",
        gradient = listOf(Color(0xFFFF6D00), Color(0xFFFF8F00), Color(0xFFFFC107)),
    ),
    OnboardPage(
        emoji = "🧹✨",
        title = "Biz temizliyoruz",
        body = "Uygulama bu cuma görsellerini cihazında otomatik bulur, sana gösterir ve " +
            "yalnızca senin onayınla siler. Böylece yer açılır, telefonun rahatlar ve " +
            "hızlanır. Hadi başlayalım!",
        gradient = listOf(Color(0xFF00C853), Color(0xFF00B0FF), Color(0xFF6C3DF4)),
    ),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardPages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(onboardPages[pagerState.currentPage].gradient)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            // Atla
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDone) {
                    Text("Atla", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                OnboardPageContent(onboardPages[page], pageOffset)
            }

            // Göstergeler
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(onboardPages.size) { i ->
                    val selected = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 11.dp else 8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (selected) 1f else 0.5f)),
                    )
                }
            }

            // Buton
            val isLast = pagerState.currentPage == onboardPages.size - 1
            Button(
                onClick = {
                    if (isLast) onDone()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 20.dp)
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = onboardPages[pagerState.currentPage].gradient.first(),
                ),
            ) {
                Text(
                    text = if (isLast) "Başla 🚀" else "İleri",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun OnboardPageContent(page: OnboardPage, pageOffset: Float) {
    // Sayfa kaydırıldıkça hafif ölçek/saydamlık geçişi (modern parallax hissi).
    val fraction = 1f - abs(pageOffset).coerceIn(0f, 1f)
    val scale = 0.85f + 0.15f * fraction

    // Emoji için yumuşak süzülme animasyonu.
    val float = rememberInfiniteTransition(label = "float")
    val dy by float.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "dy",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = 0.4f + 0.6f * fraction
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer { translationY = dy }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = page.emoji, fontSize = 72.sp)
        }
        Spacer(Modifier.height(36.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.95f),
            textAlign = TextAlign.Center,
        )
    }
}
