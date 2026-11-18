package md.psi.currencyconverter.domain

import md.psi.currencyconverter.data.RateSnapshot
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate

/** Значение показателя (курса пары) на дату. */
data class RatePoint(val date: LocalDate, val value: BigDecimal)

enum class Trend(val label: String) {
    UP("Рост"),
    DOWN("Падение"),
    FLAT("Без изменений"),
}

/** Статистика за период по точкам, упорядоченным по дате. */
data class PeriodStats(
    val count: Int,
    val first: RatePoint,
    val last: RatePoint,
    val min: RatePoint,
    val max: RatePoint,
    val average: BigDecimal,
    /** last − first. */
    val absoluteChange: BigDecimal,
    /** (last − first) / first × 100. */
    val percentChange: BigDecimal,
    val trend: Trend,
)

object Statistics {
    /** Знаков после запятой у процента изменения на экране — по нему же решается тенденция. */
    const val PERCENT_SCALE = 2
    private const val CALC_SCALE = 8

    /** Сколько знаков показывать у курсов пары: у мелких курсов (меньше 1) — 6, иначе 4. */
    fun displayScale(stats: PeriodStats): Int = if (stats.max.value < BigDecimal.ONE) 6 else 4

    /** Курс пары [from] → [to] на каждую дату, где есть обе валюты. */
    fun series(snapshots: List<RateSnapshot>, from: String, to: String): List<RatePoint> =
        snapshots.sortedBy { it.date }.mapNotNull { snapshot ->
            CurrencyConverter.rate(from, to, snapshot)
                ?.let { RatePoint(snapshot.date, it.setScale(CALC_SCALE, RoundingMode.HALF_UP)) }
        }

    /**
     * Минимум, максимум, среднее, изменение и тенденция. Для пустого набора — null.
     * Тенденция определяется по изменению в процентах, округлённому до 2 знаков, — ровно
     * так, как процент показан рядом с ней. Поэтому «Без изменений» всегда стоит рядом с
     * «0,00 %», а рост и падение — рядом с ненулевым процентом того же знака, для пар
     * с любыми по величине курсами (у MDL→EUR курс около 0,05).
     */
    fun compute(points: List<RatePoint>): PeriodStats? {
        if (points.isEmpty()) return null
        val sorted = points.sortedBy { it.date }
        val first = sorted.first()
        val last = sorted.last()
        val sum = sorted.fold(BigDecimal.ZERO) { acc, point -> acc + point.value }
        val average = sum.divide(BigDecimal(sorted.size), CALC_SCALE, RoundingMode.HALF_UP)
        val change = last.value - first.value
        val percent = if (first.value.signum() == 0) {
            BigDecimal.ZERO
        } else {
            change.multiply(BigDecimal(100)).divide(first.value, MathContext.DECIMAL128)
                .setScale(CALC_SCALE, RoundingMode.HALF_UP)
        }
        val trend = when (percent.setScale(PERCENT_SCALE, RoundingMode.HALF_UP).signum()) {
            1 -> Trend.UP
            -1 -> Trend.DOWN
            else -> Trend.FLAT
        }
        return PeriodStats(
            count = sorted.size,
            first = first,
            last = last,
            min = sorted.minBy { it.value },
            max = sorted.maxBy { it.value },
            average = average,
            absoluteChange = change,
            percentChange = percent,
            trend = trend,
        )
    }
}
