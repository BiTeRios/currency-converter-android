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
import md.psi.currencyconverter.data.RateHistory
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

/** Периоды экранов сравнения. */
enum class Period(val days: Int, val title: String) {
    WEEK(7, "Неделя"),
    MONTH(30, "Месяц"),
}

data class AnalyticsState(
    val loading: Boolean = false,
    val history: RateHistory? = null,
    /** День, на котором заканчивается загруженный период. */
    val endDate: LocalDate? = null,
    val error: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RatesRepository(
        source = BnmApi(),
        store = RateCacheStore(File(application.filesDir, "rates")),
    )

    private val _converter = MutableStateFlow(ConverterState())
    val converter: StateFlow<ConverterState> = _converter.asStateFlow()

    private val _analytics = MutableStateFlow(Period.entries.associateWith { AnalyticsState() })
    val analytics: StateFlow<Map<Period, AnalyticsState>> = _analytics.asStateFlow()

    init {
        refresh()
    }

    /** Загружает курсы за период. Без [force] не перезагружает уже загруженный сегодня период. */
    fun loadHistory(period: Period, force: Boolean = false) {
        val current = _analytics.value.getValue(period)
        val today = today()
        if (current.loading) return
        if (!force && current.history != null && current.endDate == today && current.history.problem == null) return
        updateAnalytics(period) { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val history = runCatching { repository.history(today, period.days) }
            updateAnalytics(period) { state ->
                history.fold(
                    onSuccess = { state.copy(loading = false, history = it, endDate = today) },
                    onFailure = { state.copy(loading = false, error = "Не удалось загрузить курсы: ${it.message}") },
                )
            }
        }
    }

    private fun updateAnalytics(period: Period, change: (AnalyticsState) -> AnalyticsState) =
        _analytics.update { all -> all + (period to change(all.getValue(period))) }

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
