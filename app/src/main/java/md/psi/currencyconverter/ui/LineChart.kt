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
 * Линейный график курса: подписи минимума и максимума слева, первой и последней даты снизу,
 * пунктир среднего значения. Рисуется на Canvas без сторонних библиотек.
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
            .height(220.dp)
            .semantics { contentDescription = description },
    ) {
        if (points.isEmpty()) return@Canvas
        val values = points.map { it.value.toDouble() }
        var low = values.min()
        var high = values.max()
        if (high - low < 1e-9) { // ровная линия: раздвигаем шкалу, чтобы не делить на ноль
            val pad = if (high == 0.0) 1.0 else high * 0.01
            low -= pad
            high += pad
        }

        val maxLabel = measurer.measure(Format.rate(points.maxOf { it.value }, scale), labelStyle)
        val minLabel = measurer.measure(Format.rate(points.minOf { it.value }, scale), labelStyle)
        val left = maxOf(maxLabel.size.width, minLabel.size.width) + 8.dp.toPx()
        val bottom = size.height - 22.dp.toPx()
        val top = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()

        val firstDay = points.first().date.toEpochDay()
        val span = (points.last().date.toEpochDay() - firstDay).coerceAtLeast(1)
        fun x(index: Int) = if (points.size == 1) (left + right) / 2 else
            left + (right - left) * (points[index].date.toEpochDay() - firstDay) / span
        fun y(value: Double) = (bottom - (value - low) / (high - low) * (bottom - top)).toFloat()

        // Сетка: линии минимума и максимума значений
        val yMax = y(values.max())
        val yMin = y(values.min())
        drawLine(gridColor, Offset(left, yMax), Offset(right, yMax), 1.dp.toPx())
        drawLine(gridColor, Offset(left, yMin), Offset(right, yMin), 1.dp.toPx())
        drawText(maxLabel, topLeft = Offset(0f, yMax - maxLabel.size.height / 2))
        drawText(minLabel, topLeft = Offset(0f, yMin - minLabel.size.height / 2))

        average?.let {
            val yAvg = y(it)
            drawLine(
                labelColor.copy(alpha = 0.6f), Offset(left, yAvg), Offset(right, yAvg), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
        }

        val line = Path()
        values.forEachIndexed { i, v -> if (i == 0) line.moveTo(x(i), y(v)) else line.lineTo(x(i), y(v)) }
        if (points.size > 1) {
            val fill = Path().apply {
                addPath(line)
                lineTo(x(points.lastIndex), bottom)
                lineTo(x(0), bottom)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.25f), lineColor.copy(alpha = 0f)), top, bottom))
            drawPath(line, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        values.forEachIndexed { i, v -> drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(x(i), y(v))) }

        val firstLabel = measurer.measure(Format.shortDate(points.first().date), labelStyle)
        val lastLabel = measurer.measure(Format.shortDate(points.last().date), labelStyle)
        drawText(firstLabel, topLeft = Offset(left, bottom + 4.dp.toPx()))
        if (points.size > 1) {
            drawText(lastLabel, topLeft = Offset(right - lastLabel.size.width, bottom + 4.dp.toPx()))
        }
    }
}
