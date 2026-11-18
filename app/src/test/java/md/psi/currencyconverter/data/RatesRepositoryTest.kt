package md.psi.currencyconverter.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import md.psi.currencyconverter.TestData
import md.psi.currencyconverter.TestData.valute
import md.psi.currencyconverter.TestData.xml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RatesRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val today = TestData.DAY
    private val ddmm = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private fun day(back: Long) = today.minusDays(back)
    private fun ratesFor(date: LocalDate, usd: String = "18.3383") = xml(date.format(ddmm), valute("USD", "1", usd))

    /** Поддельный источник: дата → ответ, null (не опубликовано) или исключение. */
    private class FakeSource(val answers: Map<LocalDate, Any?>) : RateSource {
        val calls = mutableListOf<LocalDate>()
        override fun fetch(date: LocalDate): ByteArray? {
            calls += date
            return when (val answer = answers[date]) {
                is ByteArray -> answer
                is Exception -> throw answer
                else -> null
            }
        }
    }

    private val offline = SourceUnavailableException("Нет связи с bnm.md: UnknownHostException")

    private fun repository(source: RateSource, store: RateCacheStore = RateCacheStore(folder.root)) =
        RatesRepository(source, store, io = Dispatchers.Unconfined)

    @Test
    fun `latest rates come from the network and are saved`() = runTest {
        val store = RateCacheStore(folder.root)
        val result = repository(FakeSource(mapOf(today to ratesFor(today))), store).latest(today)

        assertEquals(DataOrigin.NETWORK, result.origin)
        assertEquals(today, result.snapshot.date)
        assertNull(result.problem)
        assertEquals(listOf(today), store.cachedDates())
    }

    @Test
    fun `date without publication falls back to an earlier date`() = runTest {
        val source = FakeSource(mapOf(day(2) to ratesFor(day(2))))
        val result = repository(source).latest(today)

        assertEquals(day(2), result.snapshot.date)
        assertEquals(today, result.requestedDate)
        assertEquals(listOf(today, day(1), day(2)), source.calls)
    }

    @Test
    fun `empty ValCurs counts as no data`() = runTest {
        val source = FakeSource(mapOf(today to xml(today.format(ddmm)), day(1) to ratesFor(day(1))))
        assertEquals(day(1), repository(source).latest(today).snapshot.date)
    }

    @Test
    fun `no network uses the newest saved day and says why`() = runTest {
        val store = RateCacheStore(folder.root)
        store.save(day(3), ratesFor(day(3), "18.10"))
        store.save(day(5), ratesFor(day(5), "18.00"))

        val source = FakeSource(mapOf(today to offline))
        val result = repository(source, store).latest(today)

        assertEquals(DataOrigin.DEVICE, result.origin)
        assertEquals(day(3), result.snapshot.date)
        assertEquals(offline.message, result.problem)
        assertEquals("после первой ошибки сети запросов больше нет", listOf(today), source.calls)
    }

    @Test
    fun `no network and nothing saved throws NoRatesException`() {
        assertThrows(NoRatesException::class.java) {
            kotlinx.coroutines.runBlocking { repository(FakeSource(mapOf(today to offline))).latest(today) }
        }
    }

    @Test
    fun `latest asks the network first even if the day is saved`() = runTest {
        val store = RateCacheStore(folder.root)
        store.save(today, ratesFor(today, "18.00"))
        val source = FakeSource(mapOf(today to ratesFor(today, "18.3383")))
        val result = repository(source, store).latest(today)

        assertEquals(DataOrigin.NETWORK, result.origin)
        assertEquals(listOf(today), source.calls)
        assertNull(result.problem)
    }

    @Test
    fun `offline with the day saved shows saved data and the reason`() = runTest {
        val store = RateCacheStore(folder.root)
        store.save(today, ratesFor(today))
        val result = repository(FakeSource(mapOf(today to offline)), store).latest(today)

        assertEquals(DataOrigin.DEVICE, result.origin)
        assertEquals(today, result.snapshot.date)
        assertEquals(offline.message, result.problem)
    }

    @Test
    fun `fully saved period offline still reports the missing network`() = runTest {
        val store = RateCacheStore(folder.root)
        (0L..6L).forEach { store.save(day(it), ratesFor(day(it))) }
        val source = FakeSource((0L..6L).associate { day(it) to offline })

        val history = repository(source, store).history(today, 7)

        assertEquals(7, history.snapshots.size)
        assertEquals(offline.message, history.problem)
        assertEquals("из сети запрашивается только последний день", listOf(today), source.calls)
    }

    @Test
    fun `history of seven days`() = runTest {
        val answers = (0L..6L).associate { day(it) to ratesFor(day(it), "18.${it}0") }
        val history = repository(FakeSource(answers)).history(today, 7)

        assertEquals((6L downTo 0L).map { day(it) }, history.snapshots.map { it.date })
        assertTrue(history.missingDates.isEmpty())
        assertNull(history.problem)
    }

    @Test
    fun `history offline returns only saved days and the reason`() = runTest {
        val store = RateCacheStore(folder.root)
        store.save(day(1), ratesFor(day(1)))
        store.save(day(4), ratesFor(day(4)))
        val source = FakeSource((0L..6L).associate { day(it) to offline })

        val history = repository(source, store).history(today, 7)

        assertEquals(listOf(day(4), day(1)), history.snapshots.map { it.date })
        assertEquals(5, history.missingDates.size)
        assertEquals(offline.message, history.problem)
    }

    @Test
    fun `history with no data at all is empty, not an error`() = runTest {
        val history = repository(FakeSource(emptyMap())).history(today, 30)
        assertTrue(history.snapshots.isEmpty())
        assertEquals(30, history.missingDates.size)
    }

    @Test
    fun `damaged saved file is ignored and removed`() = runTest {
        val store = RateCacheStore(folder.root)
        store.save(today, "garbage".toByteArray())
        val result = repository(FakeSource(mapOf(today to ratesFor(today))), store).latest(today)

        assertEquals(DataOrigin.NETWORK, result.origin)
    }
}
