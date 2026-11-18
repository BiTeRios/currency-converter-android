package md.psi.currencyconverter.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Форматирование чисел и дат по-русски: «1 833,83», «18,3383», «+1,25 %», «18.11.2026». */
object Format {
    private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")
    private const val THIN_SPACE = ' '

    fun amount(value: BigDecimal): String = grouped(value.setScale(2, RoundingMode.HALF_UP))

    fun rate(value: BigDecimal): String {
        val rounded = value.setScale(4, RoundingMode.HALF_UP)
        return if (rounded.signum() == 0 && value.signum() > 0) "< 0,0001" else grouped(rounded)
    }

    /** Изменение со знаком: «+0,0123», «−0,0500», «0,0000». */
    fun signedRate(value: BigDecimal): String {
        val rounded = value.setScale(4, RoundingMode.HALF_UP)
        return when (rounded.signum()) {
            1 -> "+" + grouped(rounded)
            -1 -> "−" + grouped(rounded.negate())
            else -> grouped(rounded)
        }
    }

    /** Процент со знаком и двумя знаками: «+1,25 %», «−0,40 %». */
    fun percent(value: BigDecimal): String {
        val rounded = value.setScale(2, RoundingMode.HALF_UP)
        val sign = when (rounded.signum()) { 1 -> "+"; -1 -> "−"; else -> "" }
        return sign + grouped(rounded.abs()) + "${THIN_SPACE}%"
    }

    fun date(value: LocalDate): String = value.format(DATE)

    fun shortDate(value: LocalDate): String = value.format(SHORT_DATE)

    /** Разряды через неразрывный пробел, дробная часть через запятую. */
    private fun grouped(value: BigDecimal): String {
        val plain = value.toPlainString()
        val negative = plain.startsWith("-")
        val digits = plain.removePrefix("-")
        val integer = digits.substringBefore('.')
        val fraction = digits.substringAfter('.', "")
        val groupedInteger = integer.reversed().chunked(3).joinToString("$THIN_SPACE").reversed()
        return (if (negative) "-" else "") + groupedInteger + if (fraction.isEmpty()) "" else ",$fraction"
    }
}
