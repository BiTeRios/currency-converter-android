package md.psi.currencyconverter.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference

/** Откуда пришли данные: свежий ответ сети или сохранённые на устройстве дни. */
enum class DataOrigin { NETWORK, DEVICE }

/**
 * Курсы для конвертера. [problem] — почему не удалось обратиться к источнику; если он задан,
 * показаны сохранённые на устройстве данные.
 */
data class LatestRates(
    val snapshot: RateSnapshot,
    val requestedDate: LocalDate,
    val origin: DataOrigin,
    val problem: String? = null,
)

/** Курсы за период. [missingDates] — дни без данных; [problem] — источник был недоступен. */
data class RateHistory(
    val snapshots: List<RateSnapshot>,
    val missingDates: List<LocalDate>,
    val problem: String? = null,
)

/**
 * Единая точка доступа к курсам.
 * Последний день периода всегда сначала запрашивается в сети: так пользователь узнаёт,
 * что сети нет, даже если всё нужное уже сохранено. Прошедшие дни берутся из кэша на
 * устройстве (официальный курс прошедшей даты не меняется), недостающие — из сети.
 * Каждый успешный ответ сети сохраняется, поэтому данные переживают перезапуск.
 */
class RatesRepository(
    private val source: RateSource,
    private val store: RateCacheStore,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val maxParallelRequests: Int = 4,
) {

    /**
     * Курсы на [date] (обычно сегодня). Если за дату курс не публиковался, берётся ближайшая
     * более ранняя дата (до [MAX_LOOKBACK_DAYS] дней назад). Без сети — сохранённый курс
     * на эту дату или последний сохранённый день, с указанием причины в [LatestRates.problem].
     * @throws NoRatesException нет ни сети, ни сохранённых данных.
     */
    suspend fun latest(date: LocalDate): LatestRates = withContext(io) {
        val offline = AtomicReference<String?>(null)
        for (offset in 0..MAX_LOOKBACK_DAYS) {
            val day = date.minusDays(offset.toLong())
            val loaded = loadDay(day, offline, networkFirst = true)
            if (loaded != null) {
                val (snapshot, origin) = loaded
                return@withContext LatestRates(snapshot, date, origin, offline.get().takeIf { origin == DataOrigin.DEVICE })
            }
            if (offline.get() != null) break
        }
        val problem = offline.get()
        val newest = newestCached()
            ?: throw NoRatesException(
                problem?.let { "$it. Сохранённых курсов на устройстве нет." }
                    ?: "НБМ не публиковал курс за последние $MAX_LOOKBACK_DAYS дней."
            )
        LatestRates(newest, date, DataOrigin.DEVICE, problem ?: "Курс за $date не найден")
    }

    /**
     * Курсы за [days] календарных дней, заканчивая [endDate] включительно.
     * Отсутствующие в кэше дни запрашиваются параллельно; после первой ошибки сети
     * остальные дни берутся только из кэша, чтобы не ждать тайм-аутов.
     */
    suspend fun history(endDate: LocalDate, days: Int): RateHistory = coroutineScope {
        require(days > 0) { "days must be positive" }
        val offline = AtomicReference<String?>(null)
        val limiter = Semaphore(maxParallelRequests)
        val dates = (days - 1 downTo 0).map { endDate.minusDays(it.toLong()) }
        val loaded = dates.map { day ->
            async(io) {
                limiter.withPermit { day to loadDay(day, offline, networkFirst = day == endDate)?.first }
            }
        }.awaitAll()
        RateHistory(
            snapshots = loaded.mapNotNull { it.second }.sortedBy { it.date },
            missingDates = loaded.filter { it.second == null }.map { it.first },
            problem = offline.get(),
        )
    }

    /**
     * День из сети или кэша; null — за этот день данных нет.
     * [networkFirst]: сначала сеть, кэш — только если сеть недоступна.
     */
    private fun loadDay(
        day: LocalDate,
        offline: AtomicReference<String?>,
        networkFirst: Boolean,
    ): Pair<RateSnapshot, DataOrigin>? {
        if (!networkFirst || offline.get() != null) {
            cachedSnapshot(day)?.let { return it to DataOrigin.DEVICE }
            if (offline.get() != null) return null
        }
        val xml = try {
            source.fetch(day)
        } catch (e: SourceUnavailableException) {
            offline.compareAndSet(null, e.message ?: "Нет связи с источником")
            return cachedSnapshot(day)?.let { it to DataOrigin.DEVICE }
        } ?: return null
        val snapshot = try {
            BnmXmlParser.parse(xml, day)
        } catch (e: InvalidDataException) {
            offline.compareAndSet(null, "Источник вернул некорректные данные")
            return cachedSnapshot(day)?.let { it to DataOrigin.DEVICE }
        }
        if (snapshot.rates.isEmpty()) return null
        runCatching { store.save(snapshot.date, xml) } // не сохранилось — работаем без кэша
        return snapshot to DataOrigin.NETWORK
    }

    private fun cachedSnapshot(day: LocalDate): RateSnapshot? {
        val xml = store.load(day) ?: return null
        val snapshot = runCatching { BnmXmlParser.parse(xml, day) }.getOrNull()
        if (snapshot == null || snapshot.rates.isEmpty()) {
            store.delete(day) // повреждённый файл не должен мешать следующей загрузке
            return null
        }
        return snapshot
    }

    private fun newestCached(): RateSnapshot? =
        store.cachedDates().firstNotNullOfOrNull { cachedSnapshot(it) }

    companion object {
        const val MAX_LOOKBACK_DAYS = 10
    }
}
