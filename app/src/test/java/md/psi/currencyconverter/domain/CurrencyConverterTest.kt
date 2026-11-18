package md.psi.currencyconverter.domain

import md.psi.currencyconverter.TestData
import md.psi.currencyconverter.data.BnmXmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CurrencyConverterTest {
    private val snapshot = BnmXmlParser.parse(TestData.sampleXml(), TestData.DAY)

    @Test
    fun `usd to mdl and back`() {
        val there = CurrencyConverter.convert(BigDecimal("100"), "USD", "MDL", snapshot)
        assertEquals(BigDecimal("1833.83"), there.result)
        assertEquals(BigDecimal("18.3383"), there.rate)
        assertEquals(BigDecimal("0.0545"), there.reverseRate)

        val back = CurrencyConverter.convert(there.result, "MDL", "USD", snapshot)
        assertEquals(BigDecimal("100.00"), back.result)
    }

    @Test
    fun `cross rate through mdl respects nominal`() {
        // 100 EUR → JPY: 100 × 20.1538 / (11.5165 / 100)
        assertEquals(BigDecimal("17499.93"), CurrencyConverter.convert(BigDecimal("100"), "EUR", "JPY", snapshot).result)
    }

    @Test
    fun `same currency`() {
        val conversion = CurrencyConverter.convert(BigDecimal("50"), "EUR", "EUR", snapshot)
        assertTrue(conversion.sameCurrency)
        assertEquals(BigDecimal("50.00"), conversion.result)
        assertEquals(BigDecimal("1.0000"), conversion.rate)
    }

    @Test
    fun `unknown currency`() {
        assertNull(CurrencyConverter.rate("XXX", "MDL", snapshot))
        assertThrows(IllegalArgumentException::class.java) {
            CurrencyConverter.convert(BigDecimal.ONE, "XXX", "MDL", snapshot)
        }
    }

    @Test
    fun `round trip stays within rounding tolerance for every pair`() {
        // Показанный результат округлён до 0,01 целевой валюты, поэтому обратный перевод
        // может отличаться на 0,005 × курс(цель)/курс(источник) плюс 0,01 своего округления.
        val amount = BigDecimal("1000")
        for (from in snapshot.codes) for (to in snapshot.codes) {
            val there = CurrencyConverter.convert(amount, from, to, snapshot).result
            if (there.signum() == 0) continue
            val back = CurrencyConverter.convert(there, to, from, snapshot).result
            val tolerance = BigDecimal("0.01") +
                BigDecimal("0.005") * snapshot.perUnit(to)!! / snapshot.perUnit(from)!!
            assertTrue("$from→$to→$from: $back", (back - amount).abs() <= tolerance)
        }
    }
}
