package md.psi.currencyconverter

import md.psi.currencyconverter.data.CurrencyRate
import md.psi.currencyconverter.data.RateSnapshot
import java.math.BigDecimal
import java.time.LocalDate

/** Общие данные для тестов. */
object TestData {
    val DAY: LocalDate = LocalDate.of(2026, 11, 18)

    /** Настоящий ответ НБМ (русская выгрузка) за 18.11.2026, 40 валют. */
    fun sampleXml(): ByteArray =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("bnm_2026-11-18.xml")) { "нет тестового XML" }
            .use { it.readBytes() }

    fun xml(date: String, vararg valutes: String): ByteArray =
        """<?xml version="1.0" encoding="UTF-8"?><ValCurs Date="$date" name="Обменные ставки">${valutes.joinToString("")}</ValCurs>"""
            .toByteArray()

    fun valute(code: String, nominal: String, value: String, name: String = "Валюта $code") =
        "<Valute><CharCode>$code</CharCode><Nominal>$nominal</Nominal><Name>$name</Name><Value>$value</Value></Valute>"

    fun snapshot(date: LocalDate, vararg rates: Pair<String, String>): RateSnapshot =
        RateSnapshot(date, rates.associate { (code, value) -> code to CurrencyRate(code, code, 1, BigDecimal(value)) })
}
