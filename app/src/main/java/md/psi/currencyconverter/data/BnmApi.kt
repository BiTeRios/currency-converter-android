package md.psi.currencyconverter.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Откуда берутся сырые ответы с курсами. Отделено от HTTP, чтобы подменять в тестах. */
fun interface RateSource {
    /**
     * XML с курсами на дату или null, если за эту дату курс не публиковался.
     * @throws SourceUnavailableException нет сети, тайм-аут, ошибка сервера.
     */
    fun fetch(date: LocalDate): ByteArray?
}

/** Официальная XML-выгрузка Национального банка Молдовы. */
class BnmApi(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val timeoutMs: Int = 10_000,
) : RateSource {

    fun buildUrl(date: LocalDate): String = "$baseUrl?get_xml=1&date=${date.format(DATE_FORMAT)}"

    override fun fetch(date: LocalDate): ByteArray? {
        val connection = try {
            URL(buildUrl(date)).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw SourceUnavailableException("Нет связи с bnm.md: ${e.message}", e)
        }
        try {
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.setRequestProperty("User-Agent", USER_AGENT)
            return when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK ->
                    connection.inputStream.use { it.readBytes() }.takeUnless { it.isBlank() }
                HttpURLConnection.HTTP_NOT_FOUND -> null // за эту дату курс не опубликован
                else -> throw SourceUnavailableException("bnm.md ответил кодом $code")
            }
        } catch (e: SourceUnavailableException) {
            throw e
        } catch (e: IOException) {
            throw SourceUnavailableException("Нет связи с bnm.md: ${e.message ?: e.javaClass.simpleName}", e)
        } finally {
            connection.disconnect()
        }
    }

    private fun ByteArray.isBlank(): Boolean = all { it.toInt().toChar().isWhitespace() }

    companion object {
        const val DEFAULT_BASE_URL = "https://www.bnm.md/ru/official_exchange_rates"
        const val SOURCE_NAME = "Национальный банк Молдовы, bnm.md"
        private const val USER_AGENT = "CurrencyConverterAndroid/1.0 (educational project)"
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
