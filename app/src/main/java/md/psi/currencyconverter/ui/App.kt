package md.psi.currencyconverter.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import md.psi.currencyconverter.R

private enum class Tab(val title: String, val icon: Int, val period: Period?) {
    CONVERTER("Конвертер", R.drawable.ic_swap, null),
    WEEK("Неделя", R.drawable.ic_show_chart, Period.WEEK),
    MONTH("Месяц", R.drawable.ic_calendar, Period.MONTH),
}

@Composable
fun App(viewModel: MainViewModel = viewModel()) {
    val converter by viewModel.converter.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.CONVERTER) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(painterResource(item.icon), contentDescription = null) },
                        label = { Text(item.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        val period = tab.period
        if (period == null) {
            ConverterScreen(
                state = converter,
                onAmountChange = viewModel::onAmountChange,
                onFromChange = viewModel::onFromChange,
                onToChange = viewModel::onToChange,
                onSwap = viewModel::swap,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding),
            )
        } else {
            LaunchedEffect(period) { viewModel.loadHistory(period) }
            val state = analytics.getValue(period)
            AnalyticsScreen(
                period = period,
                state = state,
                from = converter.from,
                to = converter.to,
                pickerSnapshot = converter.latest?.snapshot ?: state.history?.snapshots?.lastOrNull(),
                onFromChange = viewModel::onFromChange,
                onToChange = viewModel::onToChange,
                onSwap = viewModel::swap,
                onRefresh = { viewModel.loadHistory(period, force = true) },
                modifier = Modifier.padding(padding),
            )
        }
    }
}
