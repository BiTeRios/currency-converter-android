package md.psi.currencyconverter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import md.psi.currencyconverter.R
import md.psi.currencyconverter.data.BnmApi
import md.psi.currencyconverter.data.RateSnapshot
import md.psi.currencyconverter.domain.Format
import md.psi.currencyconverter.domain.PeriodStats
import md.psi.currencyconverter.domain.Statistics
import md.psi.currencyconverter.domain.Trend
import md.psi.currencyconverter.ui.theme.TrendColors

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

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(period.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Динамика курса $from → $to за ${period.days} дней",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.loading) {
                CircularProgressIndicator(Modifier.padding(8.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onRefresh) { Text("Обновить") }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CurrencyPicker("Валюта", from, pickerSnapshot, onFromChange, Modifier.weight(1f))
            FilledTonalIconButton(onClick = onSwap, modifier = Modifier.padding(top = 18.dp)) {
                Icon(painterResource(R.drawable.ic_swap), contentDescription = "Поменять валюты местами")
            }
            CurrencyPicker("В валюте", to, pickerSnapshot, onToChange, Modifier.weight(1f))
        }

        state.error?.let { NoticeCard(it, "Повторить", onRefresh) }
        history?.let { h ->
            if (h.problem != null) {
                val actual = h.snapshots.maxOfOrNull { it.date }
                NoticeCard(
                    "${h.problem}. Показаны сохранённые на устройстве дни: ${h.snapshots.size} из ${period.days}." +
                        (actual?.let { " Данные актуальны на ${Format.date(it)}." } ?: ""),
                    "Повторить",
                    onRefresh,
                )
            }
        }

        when {
            from == to -> Text("Выберите разные валюты: курс валюты к самой себе всегда равен 1.")
            stats == null && state.loading -> Text("Загружаем курсы за ${period.days} дней…")
            stats == null && !state.loading ->
                Text("Нет данных за период. Подключитесь к интернету и нажмите «Обновить».")
            stats != null -> {
                Card(Modifier.fillMaxWidth()) {
                    LineChart(points, stats.average.toDouble(), Statistics.displayScale(stats), modifier = Modifier.padding(12.dp))
                }
                TrendBanner(stats)
                StatsGrid(stats, to)
                Text(
                    "Период: ${Format.date(stats.first.date)} – ${Format.date(stats.last.date)} · " +
                        "дней с данными: ${stats.count} из ${period.days}\n" +
                        "Данные актуальны на ${Format.date(stats.last.date)}" +
                        (if (history?.problem != null) " (сохранены на устройстве, без сети)" else "") +
                        "\nИсточник: ${BnmApi.SOURCE_NAME}" +
                        "\nПунктир на графике — среднее значение",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TrendBanner(stats: PeriodStats) {
    val (color, arrow) = when (stats.trend) {
        Trend.UP -> TrendColors.up to "▲"
        Trend.DOWN -> TrendColors.down to "▼"
        Trend.FLAT -> TrendColors.flat to "▶"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f), contentColor = color),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$arrow ${stats.trend.label}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(Format.percent(stats.percentChange), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatsGrid(stats: PeriodStats, unit: String) {
    val scale = Statistics.displayScale(stats)
    val cells = listOf(
        Triple("Минимум", "${Format.rate(stats.min.value, scale)} $unit", Format.date(stats.min.date)),
        Triple("Максимум", "${Format.rate(stats.max.value, scale)} $unit", Format.date(stats.max.date)),
        Triple("Среднее", "${Format.rate(stats.average, scale)} $unit", "за ${stats.count} дн."),
        Triple("Изменение", "${Format.signedRate(stats.absoluteChange, scale)} $unit", Format.percent(stats.percentChange)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (title, value, hint) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(hint, style = MaterialTheme.typography.bodySmall, color = Color.Unspecified)
                        }
                    }
                }
            }
        }
    }
}
