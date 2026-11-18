package md.psi.currencyconverter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import md.psi.currencyconverter.data.BnmApi
import md.psi.currencyconverter.data.LatestRates
import md.psi.currencyconverter.data.NoRatesException
import md.psi.currencyconverter.data.RateCacheStore
import md.psi.currencyconverter.data.RatesRepository
import md.psi.currencyconverter.domain.AmountCheck
import md.psi.currencyconverter.domain.AmountInput
import md.psi.currencyconverter.domain.Conversion
import md.psi.currencyconverter.domain.CurrencyConverter
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

data class ConverterState(
    val loading: Boolean = true,
    val latest: LatestRates? = null,
    /** Курсов нет совсем: ни сети, ни сохранённых. */
    val fatalError: String? = null,
    val amountText: String = "100",
    val from: String = "USD",
    val to: String = "MDL",
    val conversion: Conversion? = null,
    val inputError: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RatesRepository(
        source = BnmApi(),
        store = RateCacheStore(File(application.filesDir, "rates")),
    )

    private val _converter = MutableStateFlow(ConverterState())
    val converter: StateFlow<ConverterState> = _converter.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _converter.update { it.copy(loading = true) }
        viewModelScope.launch {
            val newState = try {
                val latest = repository.latest(today())
                _converter.value.copy(loading = false, latest = latest, fatalError = null)
            } catch (e: NoRatesException) {
                _converter.value.copy(loading = false, fatalError = e.message)
            }
            _converter.value = recalculate(newState)
        }
    }

    fun onAmountChange(text: String) = _converter.update { recalculate(it.copy(amountText = text)) }

    fun onFromChange(code: String) = _converter.update { recalculate(it.copy(from = code)) }

    fun onToChange(code: String) = _converter.update { recalculate(it.copy(to = code)) }

    fun swap() = _converter.update { recalculate(it.copy(from = it.to, to = it.from)) }

    private fun recalculate(state: ConverterState): ConverterState {
        val snapshot = state.latest?.snapshot ?: return state.copy(conversion = null, inputError = null)
        return when (val check = AmountInput.check(state.amountText)) {
            is AmountCheck.Invalid -> state.copy(conversion = null, inputError = check.message)
            is AmountCheck.Valid -> {
                val conversion = runCatching {
                    CurrencyConverter.convert(check.value, state.from, state.to, snapshot)
                }.getOrNull()
                state.copy(
                    conversion = conversion,
                    inputError = if (conversion == null) "Выберите валюты из списка" else null,
                )
            }
        }
    }

    companion object {
        val ZONE: ZoneId = ZoneId.of("Europe/Chisinau")
        fun today(): LocalDate = LocalDate.now(ZONE)
    }
}
