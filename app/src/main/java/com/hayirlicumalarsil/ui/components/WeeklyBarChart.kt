package com.hayirlicumalarsil.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hayirlicumalarsil.formatBytes
import com.hayirlicumalarsil.ui.theme.VividPink
import com.hayirlicumalarsil.ui.theme.VividPurple

/**
 * Son haftalarda kazanılan alanı gösteren, gradyan dolgulu bar grafik.
 * [data]: (hafta etiketi, silinen bayt) çiftleri, eskiden yeniye sıralı.
 */
@Composable
fun WeeklyBarChart(data: List<Pair<String, Long>>, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor, fontSize = 9.sp)
    val valueStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.primary,
        fontSize = 9.sp,
    )
    val animatedFraction by animateFloatAsState(
        targetValue = if (data.any { it.second > 0 }) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "chartGrow",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp),
    ) {
        if (data.isEmpty()) return@Canvas
        val maxValue = data.maxOf { it.second }.coerceAtLeast(1L)
        val labelSpace = 40f
        val valueSpace = 34f
        val chartHeight = size.height - labelSpace - valueSpace
        val slotWidth = size.width / data.size
        val barWidth = slotWidth * 0.55f

        data.forEachIndexed { index, (label, value) ->
            val barHeight = (value.toFloat() / maxValue) * chartHeight * animatedFraction
            val left = index * slotWidth + (slotWidth - barWidth) / 2f
            val top = valueSpace + (chartHeight - barHeight)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(VividPink, VividPurple),
                    startY = top,
                    endY = top + barHeight.coerceAtLeast(1f),
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight.coerceAtLeast(if (value > 0) 6f else 0f)),
                cornerRadius = CornerRadius(8f, 8f),
            )

            val labelLayout = textMeasurer.measure(AnnotatedString(label), labelStyle)
            drawText(
                textLayoutResult = labelLayout,
                topLeft = Offset(
                    index * slotWidth + (slotWidth - labelLayout.size.width) / 2f,
                    size.height - labelSpace + 8f,
                ),
            )

            if (value > 0) {
                val valueLayout = textMeasurer.measure(AnnotatedString(formatBytes(value)), valueStyle)
                drawText(
                    textLayoutResult = valueLayout,
                    topLeft = Offset(
                        index * slotWidth + (slotWidth - valueLayout.size.width) / 2f,
                        (top - valueLayout.size.height - 4f).coerceAtLeast(0f),
                    ),
                )
            }
        }
    }
}
