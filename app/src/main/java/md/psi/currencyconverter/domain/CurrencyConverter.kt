package md.psi.currencyconverter.domain

import md.psi.currencyconverter.data.RateSnapshot
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Результат конвертации: сумма округлена до копеек, курсы — до 4 знаков. */
data class Conversion(
    val amount: BigDecimal,
    val from: String,
    val to: String,
    val result: BigDecimal,
    val rate: BigDecimal,
    val reverseRate: BigDecimal,
) {
    val sameCurrency: Boolean get() = from == to
}

object CurrencyConverter {
    private val MC = MathContext.DECIMAL128

    /** Курс пары: сколько единиц [to] дают за одну единицу [from]. null — валюты нет в курсах. */
    fun rate(from: String, to: String, snapshot: RateSnapshot): BigDecimal? {
        val fromPerUnit = snapshot.perUnit(from) ?: return null
        val toPerUnit = snapshot.perUnit(to) ?: return null
        return fromPerUnit.divide(toPerUnit, MC)
    }

    /**
     * Перевод через лей: amount × курс(from) / курс(to).
     * @throws IllegalArgumentException если какой-то валюты нет в [snapshot].
     */
    fun convert(amount: BigDecimal, from: String, to: String, snapshot: RateSnapshot): Conversion {
        val direct = rate(from, to, snapshot)
            ?: throw IllegalArgumentException("Нет курса для пары $from/$to")
        val reverse = BigDecimal.ONE.divide(direct, MC)
        return Conversion(
            amount = amount,
            from = from,
            to = to,
            result = amount.multiply(direct, MC).setScale(2, RoundingMode.HALF_UP),
            rate = direct.setScale(4, RoundingMode.HALF_UP),
            reverseRate = reverse.setScale(4, RoundingMode.HALF_UP),
        )
    }
}
