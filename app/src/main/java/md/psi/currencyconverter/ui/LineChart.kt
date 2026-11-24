package md.psi.currencyconverter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import md.psi.currencyconverter.domain.Format
import md.psi.currencyconverter.domain.RatePoint

/**
 * Точки стоят на своих календарных датах. Пропуски выделены коротким пунктиром.
 */
@Composable
fun LineChart(points: List<RatePoint>, average: Double?, scale: Int = 4, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = labelColor)
    val description = if (points.isEmpty()) "Нет данных" else
        "График курса с ${Format.date(points.first().date)} по ${Format.date(points.last().date)}"

    Canvas(
        modifier
            .fillMaxWidth()
            .height(166.dp)
            .semantics { contentDescription = description },
    ) {
        if (points.isEmpty()) return@Canvas
        val values = points.map { it.value.toDouble() }
        val minValue = values.min()
        val maxValue = values.max()
        val flat = maxValue - minValue < 1e-9
        val pad = if (flat) (maxValue * 0.01).coerceAtLeast(1e-9) else 0.0
        val low = minValue - pad
        val high = maxValue + pad

        val maxLabel = measurer.measure(Format.rate(points.maxOf { it.value }, scale), labelStyle)
        val minLabel = measurer.measure(Format.rate(points.minOf { it.value }, scale), labelStyle)
        val left = maxOf(maxLabel.size.width, minLabel.size.width) + 8.dp.toPx()
        val firstLabel = measurer.measure(Format.shortDate(points.first().date), labelStyle)
        val lastLabel = measurer.measure(Format.shortDate(points.last().date), labelStyle)
        val bottom = size.height - firstLabel.size.height - 8.dp.toPx()
        val top = maxOf(maxLabel.size.height, minLabel.size.height) / 2f + 3.dp.toPx()
        val right = size.width - 5.dp.toPx()
        if (right <= left || bottom <= top) return@Canvas

        val firstDay = points.first().date.toEpochDay()
        val span = (points.last().date.toEpochDay() - firstDay).coerceAtLeast(1)
        fun x(index: Int) = if (points.size == 1) (left + right) / 2 else
            left + (right - left) * (points[index].date.toEpochDay() - firstDay) / span
        fun y(value: Double) = (bottom - (value - low) / (high - low) * (bottom - top)).toFloat()

        val yMax = y(maxValue)
        val yMin = y(minValue)
        drawLine(gridColor, Offset(left, yMax), Offset(right, yMax), 1.dp.toPx())
        drawText(maxLabel, topLeft = Offset(0f, yMax - maxLabel.size.height / 2))
        if (!flat && maxLabel.size.height <= yMin - yMax) {
            drawLine(gridColor, Offset(left, yMin), Offset(right, yMin), 1.dp.toPx())
            drawText(minLabel, topLeft = Offset(0f, yMin - minLabel.size.height / 2))
        }

        average?.let {
            val yAvg = y(it)
            drawLine(
                labelColor.copy(alpha = 0.6f), Offset(left, yAvg), Offset(right, yAvg), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
        }

        if (points.size > 1) {
            val line = Path().apply { moveTo(x(0), y(values.first())) }
            var hasGap = false
            val gapEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
            for (index in 1..points.lastIndex) {
                val end = Offset(x(index), y(values[index]))
                if (points[index].date.toEpochDay() - points[index - 1].date.toEpochDay() > 1) {
                    hasGap = true
                    val start = Offset(x(index - 1), y(values[index - 1]))
                    drawLine(lineColor.copy(alpha = 0.55f), start, end, 2.dp.toPx(), pathEffect = gapEffect)
                    line.moveTo(end.x, end.y)
                } else {
                    line.lineTo(end.x, end.y)
                }
            }
            if (!hasGap) {
                val fill = Path().apply {
                    addPath(line)
                    lineTo(x(points.lastIndex), bottom)
                    lineTo(x(0), bottom)
                    close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.19f), lineColor.copy(alpha = 0.01f)), top, bottom))
            }
            drawPath(line, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        val dotRadius = if (points.size > 10) 2.dp.toPx() else 3.dp.toPx()
        values.forEachIndexed { i, v -> drawCircle(lineColor, radius = dotRadius, center = Offset(x(i), y(v))) }

        val firstLabelX = if (points.size == 1) x(0) - firstLabel.size.width / 2f else left
        drawText(firstLabel, topLeft = Offset(firstLabelX, bottom + 4.dp.toPx()))
        if (points.size > 1 && right - left > firstLabel.size.width + lastLabel.size.width + 4.dp.toPx()) {
            drawText(lastLabel, topLeft = Offset(right - lastLabel.size.width, bottom + 4.dp.toPx()))
        }
    }
}
