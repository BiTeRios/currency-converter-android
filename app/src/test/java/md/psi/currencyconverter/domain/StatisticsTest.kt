package md.psi.currencyconverter.domain

import md.psi.currencyconverter.TestData
import md.psi.currencyconverter.TestData.snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class StatisticsTest {

    private fun points(vararg values: String): List<RatePoint> =
        values.mapIndexed { i, v -> RatePoint(LocalDate.of(2026, 11, 1).plusDays(i.toLong()), BigDecimal(v)) }

    private fun assertDecimal(expected: String, actual: BigDecimal) =
        assertEquals("ожидалось $expected, получено $actual", 0, BigDecimal(expected).compareTo(actual))

    @Test
    fun `min max average and change`() {
        val stats = Statistics.compute(points("18.00", "18.50", "17.80", "18.20"))!!

        assertDecimal("17.80", stats.min.value)
        assertEquals(LocalDate.of(2026, 11, 3), stats.min.date)
        assertDecimal("18.50", stats.max.value)
        assertEquals(LocalDate.of(2026, 11, 2), stats.max.date)
        assertDecimal("18.125", stats.average)
        assertDecimal("0.20", stats.absoluteChange)
        assertDecimal("1.11111111", stats.percentChange) // 0.2 / 18 * 100
        assertEquals(Trend.UP, stats.trend)
        assertEquals(4, stats.count)
    }

    @Test
    fun `falling rate`() {
        val stats = Statistics.compute(points("20.00", "19.00"))!!
        assertDecimal("-1.00", stats.absoluteChange)
        assertDecimal("-5", stats.percentChange)
        assertEquals(Trend.DOWN, stats.trend)
    }

    @Test
    fun `no change when first and last are equal`() {
        val stats = Statistics.compute(points("18.3383", "18.9", "18.3383"))!!
        assertDecimal("0", stats.absoluteChange)
        assertDecimal("0", stats.percentChange)
        assertEquals(Trend.FLAT, stats.trend)
    }

    @Test
    fun `change below displayed precision counts as no change`() {
        val stats = Statistics.compute(points("18.33830000", "18.33830004"))!!
        assertEquals(Trend.FLAT, stats.trend)
    }

    @Test
    fun `points are sorted by date before computing`() {
        val shuffled = points("10", "11", "12").reversed()
        val stats = Statistics.compute(shuffled)!!
        assertDecimal("10", stats.first.value)
        assertDecimal("12", stats.last.value)
        assertEquals(Trend.UP, stats.trend)
    }

    @Test
    fun `single point`() {
        val stats = Statistics.compute(points("18.3383"))!!
        assertDecimal("18.3383", stats.average)
        assertDecimal("0", stats.percentChange)
        assertEquals(Trend.FLAT, stats.trend)
    }

    @Test
    fun `empty data set gives null`() {
        assertNull(Statistics.compute(emptyList()))
        assertNull(Statistics.compute(Statistics.series(emptyList(), "USD", "MDL")))
    }

    @Test
    fun `series of a pair skips days without one of the currencies`() {
        val day1 = LocalDate.of(2026, 11, 17)
        val snapshots = listOf(
            snapshot(TestData.DAY, "USD" to "18.3383", "EUR" to "20.1538"),
            snapshot(day1, "USD" to "18.30"), // нет EUR
        )
        val usdToMdl = Statistics.series(snapshots, "USD", "MDL")
        val eurToUsd = Statistics.series(snapshots, "EUR", "USD")

        assertEquals(listOf(day1, TestData.DAY), usdToMdl.map { it.date })
        assertEquals(listOf(TestData.DAY), eurToUsd.map { it.date })
        assertDecimal("1.09900045", eurToUsd.single().value) // 20.1538 / 18.3383
    }
}
