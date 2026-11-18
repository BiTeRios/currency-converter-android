package md.psi.currencyconverter.data

import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

/** Официальный курс одной валюты: [value] леев за [nominal] единиц (у иены nominal = 100). */
data class CurrencyRate(
    val code: String,
    val name: String,
    val nominal: Int,
    val value: BigDecimal,
) {
    /** Сколько леев стоит одна единица валюты. */
    val perUnit: BigDecimal
        get() = value.divide(BigDecimal(nominal), MathContext.DECIMAL128)
}

/** Курсы всех валют на одну дату. Лей (MDL) — базовая валюта, в [rates] не хранится. */
data class RateSnapshot(
    val date: LocalDate,
    val rates: Map<String, CurrencyRate>,
) {
    /** MDL, EUR, USD первыми, остальные по алфавиту. */
    val codes: List<String>
        get() {
            val priority = listOf(MDL, "EUR", "USD").filter { it == MDL || it in rates }
            return priority + rates.keys.filter { it !in priority }.sorted()
        }

    fun perUnit(code: String): BigDecimal? = if (code == MDL) BigDecimal.ONE else rates[code]?.perUnit

    fun nameOf(code: String): String = if (code == MDL) MDL_NAME else rates[code]?.name ?: code

    companion object {
        const val MDL = "MDL"
        const val MDL_NAME = "Молдавский лей"
    }
}

/** Источник недоступен: нет сети, тайм-аут, ошибка сервера. */
class SourceUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Источник ответил, но данные не разобрать. */
class InvalidDataException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Курса нет ни в сети, ни на устройстве. */
class NoRatesException(message: String, cause: Throwable? = null) : Exception(message, cause)
