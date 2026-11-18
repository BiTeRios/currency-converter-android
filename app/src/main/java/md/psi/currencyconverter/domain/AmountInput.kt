package md.psi.currencyconverter.domain

import java.math.BigDecimal

/** Итог проверки введённой суммы: либо число, либо понятное сообщение. */
sealed interface AmountCheck {
    data class Valid(val value: BigDecimal) : AmountCheck
    data class Invalid(val message: String) : AmountCheck
}

/**
 * Проверка суммы. Принимает «1500», «99,95», « 1 500.5 »; отклоняет пустое поле, буквы,
 * экспоненту, ноль, отрицательные числа, больше двух знаков после запятой и слишком большие суммы.
 */
object AmountInput {
    const val EMPTY = "Введите сумму"
    const val NOT_A_NUMBER = "Введите число, например 150 или 99,95"
    const val NOT_POSITIVE = "Сумма должна быть больше нуля"
    const val TOO_PRECISE = "Не больше двух знаков после запятой"
    const val TOO_LARGE = "Сумма не должна превышать 1 000 000 000 000"

    val MAX: BigDecimal = BigDecimal("1000000000000")
    private val NUMBER = Regex("""^-?\d+(\.\d+)?$""")
    private val SPACE_BETWEEN_DIGITS = Regex("""(?<=\d)[\s  ]+(?=\d)""")

    fun check(text: String): AmountCheck {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return AmountCheck.Invalid(EMPTY)
        val normalized = trimmed.replace(SPACE_BETWEEN_DIGITS, "").replace(',', '.')
        if (!NUMBER.matches(normalized)) return AmountCheck.Invalid(NOT_A_NUMBER)
        val value = BigDecimal(normalized)
        return when {
            value.signum() <= 0 -> AmountCheck.Invalid(NOT_POSITIVE)
            normalized.substringAfter('.', "").length > 2 -> AmountCheck.Invalid(TOO_PRECISE)
            value > MAX -> AmountCheck.Invalid(TOO_LARGE)
            else -> AmountCheck.Valid(value)
        }
    }
}
