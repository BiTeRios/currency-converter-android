package md.psi.currencyconverter.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class FormatTest {
    private val nbsp = ' '

    @Test
    fun amounts() {
        assertEquals("1${nbsp}833,83", Format.amount(BigDecimal("1833.83")))
        assertEquals("1${nbsp}000${nbsp}000,00", Format.amount(BigDecimal("1000000")))
        assertEquals("0,01", Format.amount(BigDecimal("0.005"))) // округление вверх от половины
        assertEquals("12,00", Format.amount(BigDecimal("12")))
    }

    @Test
    fun rates() {
        assertEquals("18,3383", Format.rate(BigDecimal("18.3383")))
        assertEquals("0,0545", Format.rate(BigDecimal("0.054530")))
        assertEquals("< 0,0001", Format.rate(BigDecimal("0.00002")))
        assertEquals("0,049600", Format.rate(BigDecimal("0.0496"), 6))
        assertEquals("< 0,000001", Format.rate(BigDecimal("0.0000002"), 6))
        assertEquals("+0,2334", Format.signedRate(BigDecimal("0.23340")))
        assertEquals("−0,0500", Format.signedRate(BigDecimal("-0.05")))
        assertEquals("0,0000", Format.signedRate(BigDecimal.ZERO))
    }

    @Test
    fun percents() {
        assertEquals("+1,29${nbsp}%", Format.percent(BigDecimal("1.2873")))
        assertEquals("−0,40${nbsp}%", Format.percent(BigDecimal("-0.4")))
        assertEquals("0,00${nbsp}%", Format.percent(BigDecimal.ZERO))
    }

    @Test
    fun dates() {
        assertEquals("05.01.2026", Format.date(LocalDate.of(2026, 1, 5)))
        assertEquals("05.01", Format.shortDate(LocalDate.of(2026, 1, 5)))
    }
}
