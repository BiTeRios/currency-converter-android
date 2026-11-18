package md.psi.currencyconverter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import md.psi.currencyconverter.R
import md.psi.currencyconverter.data.BnmApi
import md.psi.currencyconverter.data.DataOrigin
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
    val snapshot = state.latest?.snapshot
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Конвертер", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (state.loading) {
                CircularProgressIndicator(Modifier.padding(8.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onRefresh) { Text("Обновить") }
            }
        }

        state.fatalError?.let { NoticeCard(it, "Повторить", onRefresh) }
        state.latest?.problem?.let { problem ->
            NoticeCard(
                "$problem. Показан сохранённый курс на ${Format.date(state.latest.snapshot.date)}.",
                "Повторить",
                onRefresh,
            )
        }

        OutlinedTextField(
            value = state.amountText,
            onValueChange = onAmountChange,
            label = { Text("Сумма") },
            singleLine = true,
            isError = state.inputError != null,
            supportingText = { state.inputError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CurrencyPicker("Из", state.from, snapshot, onFromChange, Modifier.weight(1f))
            FilledTonalIconButton(onClick = onSwap, modifier = Modifier.padding(top = 18.dp)) {
                Icon(painterResource(R.drawable.ic_swap), contentDescription = "Поменять валюты местами")
            }
            CurrencyPicker("В", state.to, snapshot, onToChange, Modifier.weight(1f))
        }

        state.conversion?.let { conversion ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${Format.amount(conversion.amount)} ${conversion.from} =", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (conversion.result.signum() == 0) "< 0,01 ${conversion.to}"
                        else "${Format.amount(conversion.result)} ${conversion.to}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (conversion.sameCurrency) {
                        Text("Валюты совпадают — конвертация не требуется", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("1 ${conversion.from} = ${Format.rate(conversion.rate)} ${conversion.to}")
                        Text("1 ${conversion.to} = ${Format.rate(conversion.reverseRate)} ${conversion.from}")
                    }
                }
            }
        }

        state.latest?.let { latest ->
            val origin = when (latest.origin) {
                DataOrigin.NETWORK -> "загружен сейчас"
                DataOrigin.DEVICE -> "сохранён на устройстве"
            }
            Text(
                "Официальный курс на ${Format.date(latest.snapshot.date)} · $origin\nИсточник: ${BnmApi.SOURCE_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (latest.snapshot.date != latest.requestedDate && latest.problem == null) {
                Text(
                    "За ${Format.date(latest.requestedDate)} курс не опубликован, использован курс за ${Format.date(latest.snapshot.date)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
