package com.hayirlicumalarsil.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Değeri sayarak artan/azalan metin — istatistik kartlarında kullanılır. */
@Composable
fun AnimatedCountText(
    value: Int,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 900),
        label = "animatedCount",
    )
    Text(
        text = animated.toString(),
        style = style,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier,
    )
}

/** Aday görsellerin köşesindeki yüzde rozetini çizer. */
@Composable
fun ScoreBadge(score: Int, modifier: Modifier = Modifier) {
    val background = when {
        score >= 80 -> Color(0xFFD32F2F)
        score >= 60 -> Color(0xFFEF6C00)
        else -> Color(0xFF546E7A)
    }
    Surface(
        color = background,
        contentColor = Color.White,
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Text(
            text = "%$score",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
