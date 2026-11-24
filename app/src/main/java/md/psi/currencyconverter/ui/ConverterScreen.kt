package md.psi.currencyconverter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import md.psi.currencyconverter.data.BnmApi
import md.psi.currencyconverter.data.DataOrigin
import md.psi.currencyconverter.domain.Conversion
import md.psi.currencyconverter.domain.Format

@Composable
fun ConverterScreen(
    state: ConverterState,
    onAmountChange: (String) -> Unit,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onSwap: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latest = state.latest
    val snapshot = latest?.snapshot
    ScreenContent(modifier) {
        ScreenHeading("Конвертер", state.loading, onRefresh, "По официальному курсу НБМ")

        state.fatalError?.let { NoticeCard(it, onRefresh, isError = true) }
        latest?.problem?.let { problem ->
            NoticeCard("$problem. Показан сохранённый курс на ${Format.date(latest.snapshot.date)}.", onRefresh)
        }

        Panel {
            OutlinedTextField(
                value = state.amountText,
                onValueChange = onAmountChange,
                label = { Text("Сумма") },
                singleLine = true,
                isError = state.inputError != null,
                supportingText = state.inputError?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.medium,
                textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            CurrencyPairRow(state.from, state.to, snapshot, onFromChange, onToChange, onSwap)
        }

        state.conversion?.let { ConversionResult(it) }

        if (latest != null) {
            val origin = when (latest.origin) {
                DataOrigin.NETWORK -> "загружен сейчас"
                DataOrigin.DEVICE -> "сохранён на устройстве"
            }
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Курс на ${Format.date(latest.snapshot.date)} · $origin",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(BnmApi.SOURCE_NAME, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (latest.snapshot.date != latest.requestedDate && latest.problem == null) {
                    Text(
                        "За ${Format.date(latest.requestedDate)} курс не опубликован, использован курс за ${Format.date(latest.snapshot.date)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversionResult(conversion: Conversion) {
    val colors = MaterialTheme.colorScheme
    val result = if (conversion.result.signum() == 0) "< 0,01" else Format.amount(conversion.result)
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                "${Format.amount(conversion.amount)} ${conversion.from} =",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                "$result ${conversion.to}",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 40.sp),
                maxLines = 1,
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.onPrimaryContainer.copy(alpha = 0.2f))
            if (conversion.sameCurrency) {
                Text("Валюты совпадают, конвертация не требуется", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("1 ${conversion.from} = ${Format.rate(conversion.rate)} ${conversion.to}", style = MaterialTheme.typography.bodyMedium)
                Text("1 ${conversion.to} = ${Format.rate(conversion.reverseRate)} ${conversion.from}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
