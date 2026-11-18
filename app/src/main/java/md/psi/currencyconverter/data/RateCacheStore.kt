package md.psi.currencyconverter.data

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate

/**
 * Хранилище на устройстве: сырой XML ответа НБМ на каждую дату (rates_2026-11-18.xml).
 * Официальный курс за прошедшую дату не меняется, поэтому однажды загруженный день
 * больше не запрашивается, а при отсутствии сети графики строятся из сохранённых дней.
 */
class RateCacheStore(private val dir: File) {

    fun load(date: LocalDate): ByteArray? {
        val file = fileFor(date)
        return try {
            if (file.isFile) file.readBytes() else null
        } catch (e: IOException) {
            null
        }
    }

    /** Атомарная запись: временный файл + переименование, чтобы не оставить полфайла. */
    fun save(date: LocalDate, xml: ByteArray) {
        dir.mkdirs()
        // Уникальное имя: конвертер и экран аналитики могут сохранять один день одновременно.
        val tmp = File.createTempFile("${fileFor(date).name}.", ".tmp", dir)
        try {
            tmp.writeBytes(xml)
            Files.move(tmp.toPath(), fileFor(date).toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            tmp.delete() // после успешного move файла уже нет
        }
    }

    fun delete(date: LocalDate) {
        fileFor(date).delete()
    }

    /** Даты, за которые есть сохранённые курсы, от новых к старым. */
    fun cachedDates(): List<LocalDate> =
        dir.listFiles().orEmpty()
            .mapNotNull { file ->
                FILE_NAME.matchEntire(file.name)?.groupValues?.get(1)
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            }
            .sortedDescending()

    private fun fileFor(date: LocalDate) = File(dir, "rates_$date.xml")

    private companion object {
        val FILE_NAME = Regex("""rates_(\d{4}-\d{2}-\d{2})\.xml""")
    }
}
