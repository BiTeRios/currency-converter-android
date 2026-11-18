package md.psi.currencyconverter.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun App(viewModel: MainViewModel = viewModel()) {
    val converter by viewModel.converter.collectAsStateWithLifecycle()
    Scaffold { padding ->
        ConverterScreen(
            state = converter,
            onAmountChange = viewModel::onAmountChange,
            onFromChange = viewModel::onFromChange,
            onToChange = viewModel::onToChange,
            onSwap = viewModel::swap,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
        )
    }
}
