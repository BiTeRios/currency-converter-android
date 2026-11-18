package md.psi.currencyconverter.data

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Разбор XML официальных курсов НБМ:
 * <ValCurs Date="18.11.2026"><Valute><CharCode>USD</CharCode><Nominal>1</Nominal>
 * <Name>Доллар США</Name><Value>18.3383</Value></Valute>…</ValCurs>
 */
object BnmXmlParser {
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val CODE = Regex("^[A-Z]{3}$")

    /**
     * Возвращает курсы на дату из атрибута Date (или [requestedDate], если его нет).
     * Битые элементы Valute пропускаются; список курсов может оказаться пустым.
     * @throws InvalidDataException если XML не разбирается или корень не ValCurs.
     */
    fun parse(xml: ByteArray, requestedDate: LocalDate): RateSnapshot {
        val root = try {
            val factory = DocumentBuilderFactory.newInstance()
            // Защита от XXE. Парсер Android может не знать первую настройку,
            // поэтому дополнительно запрещаем раскрытие сущностей.
            runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            factory.isExpandEntityReferences = false
            factory.newDocumentBuilder().parse(ByteArrayInputStream(xml)).documentElement
        } catch (e: Exception) {
            throw InvalidDataException("Ответ источника не является корректным XML", e)
        }
        if (root == null || root.tagName != "ValCurs") {
            throw InvalidDataException("Неожиданный формат ответа: корневой элемент ${root?.tagName}")
        }

        val date = root.getAttribute("Date").trim().takeIf { it.isNotEmpty() }
            ?.let { runCatching { LocalDate.parse(it, DATE_FORMAT) }.getOrNull() }
            ?: requestedDate

        val rates = LinkedHashMap<String, CurrencyRate>()
        val nodes = root.getElementsByTagName("Valute")
        for (i in 0 until nodes.length) {
            val rate = parseValute(nodes.item(i) as Element) ?: continue
            rates.putIfAbsent(rate.code, rate)
        }
        return RateSnapshot(date, rates)
    }

    private fun parseValute(element: Element): CurrencyRate? {
        val code = element.childText("CharCode")?.uppercase() ?: return null
        if (!CODE.matches(code) || code == RateSnapshot.MDL) return null
        val nominal = element.childText("Nominal")?.toIntOrNull()?.takeIf { it >= 1 } ?: return null
        val value = element.childText("Value")?.replace(',', '.')?.toBigDecimalOrNull()
            ?.takeIf { it.signum() > 0 } ?: return null
        return CurrencyRate(code, element.childText("Name").orEmpty(), nominal, value)
    }

    private fun Element.childText(tag: String): String? =
        getElementsByTagName(tag).item(0)?.textContent?.trim()?.takeIf { it.isNotEmpty() }
}
