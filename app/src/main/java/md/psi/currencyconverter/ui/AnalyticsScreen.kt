package md.psi.currencyconverter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import md.psi.currencyconverter.data.BnmApi
import md.psi.currencyconverter.data.RateSnapshot
import md.psi.currencyconverter.domain.Format
import md.psi.currencyconverter.domain.PeriodStats
import md.psi.currencyconverter.domain.Statistics
import md.psi.currencyconverter.domain.Trend
import md.psi.currencyconverter.ui.theme.trendColor

@Composable
fun AnalyticsScreen(
    period: Period,
    state: AnalyticsState,
    from: String,
    to: String,
    pickerSnapshot: RateSnapshot?,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onSwap: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val history = state.history
    val points = remember(history, from, to) {
        history?.let { Statistics.series(it.snapshots, from, to) }.orEmpty()
    }
    val stats = remember(points) { Statistics.compute(points) }

    ScreenContent(modifier) {
        ScreenHeading(period.title, state.loading, onRefresh, "$from → $to · ${period.days} дней")
        CurrencyPairRow(from, to, pickerSnapshot, onFromChange, onToChange, onSwap)

        state.error?.let { NoticeCard(it, onRefresh, isError = true) }
        history?.problem?.let { problem ->
            val actual = history.snapshots.maxOfOrNull { it.date }
            NoticeCard(
                "$problem. Показаны сохранённые на устройстве дни: ${history.snapshots.size} из ${period.days}." +
                    (actual?.let { " Данные актуальны на ${Format.date(it)}." } ?: ""),
                onRefresh,
            )
        }

        when {
            from == to -> Panel { Text("Выберите разные валюты: курс валюты к самой себе всегда равен 1.") }
            stats == null && state.loading -> Panel { Text("Загружаем курсы за ${period.days} дней…") }
            stats == null -> Panel { Text("Нет данных за период. Подключитесь к интернету и нажмите «Обновить».") }
            else -> {
                Panel {
                    Text("Курс 1 $from в $to", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    LineChart(points, stats.average.toDouble(), Statistics.displayScale(stats))
                    Text("┄  Среднее значение", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TrendBanner(stats)
                StatsGrid(stats, to)
                Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val style = MaterialTheme.typography.bodySmall
                    val color = MaterialTheme.colorScheme.onSurfaceVariant
                    Text("${Format.date(stats.first.date)} – ${Format.date(stats.last.date)} · ${stats.count} из ${period.days} дней с данными", style = style, color = color)
                    Text(
                        "Данные актуальны на ${Format.date(stats.last.date)}" +
                            (if (history?.problem != null) " (часть дней — из сохранённых на устройстве)" else ""),
                        style = style,
                        color = color,
                    )
                    Text(BnmApi.SOURCE_NAME, style = style, color = color)
                }
            }
        }
    }
}

@Composable
private fun TrendBanner(stats: PeriodStats) {
    val color = trendColor(stats.trend)
    val arrow = when (stats.trend) {
        Trend.UP -> "▲"
        Trend.DOWN -> "▼"
        Trend.FLAT -> "▶"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f), contentColor = color),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$arrow ${stats.trend.label}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(Format.percent(stats.percentChange), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatsGrid(stats: PeriodStats, unit: String) {
    val scale = Statistics.displayScale(stats)
    val cards = listOf(
        Triple("Минимум", "${Format.rate(stats.min.value, scale)} $unit", Format.date(stats.min.date)),
        Triple("Максимум", "${Format.rate(stats.max.value, scale)} $unit", Format.date(stats.max.date)),
        Triple("Среднее", "${Format.rate(stats.average, scale)} $unit", "за ${stats.count} дн."),
        Triple("Изменение", "${Format.signedRate(stats.absoluteChange, scale)} $unit", Format.percent(stats.percentChange)),
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 310.dp || LocalDensity.current.fontScale > 1.3f) 1 else 2
        val colors = MaterialTheme.colorScheme
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cards.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (title, value, hint) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = colors.surface, contentColor = colors.onSurface),
                            border = BorderStroke(1.dp, colors.outlineVariant),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(title, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(hint, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
