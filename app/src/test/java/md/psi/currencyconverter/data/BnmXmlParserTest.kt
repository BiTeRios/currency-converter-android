package md.psi.currencyconverter.data

import md.psi.currencyconverter.TestData
import md.psi.currencyconverter.TestData.valute
import md.psi.currencyconverter.TestData.xml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class BnmXmlParserTest {

    @Test
    fun `parses the real BNM response`() {
        val snapshot = BnmXmlParser.parse(TestData.sampleXml(), LocalDate.of(2000, 1, 1))

        assertEquals(TestData.DAY, snapshot.date) // дата берётся из атрибута Date
        assertEquals(40, snapshot.rates.size)
        assertEquals(BigDecimal("20.1538"), snapshot.rates.getValue("EUR").value)
        assertEquals("Евро", snapshot.rates.getValue("EUR").name)
        assertEquals(BigDecimal("18.3383"), snapshot.rates.getValue("USD").value)
        assertFalse("MDL не должен храниться в курсах", "MDL" in snapshot.rates)
    }

    @Test
    fun `nominal is taken into account`() {
        val snapshot = BnmXmlParser.parse(TestData.sampleXml(), TestData.DAY)
        val jpy = snapshot.rates.getValue("JPY")

        assertEquals(100, jpy.nominal)
        assertEquals(0, BigDecimal("0.115165").compareTo(jpy.perUnit))
        assertEquals(0, BigDecimal("0.001526").compareTo(snapshot.perUnit("UZS")))
    }

    @Test
    fun `mdl is the base currency`() {
        val snapshot = BnmXmlParser.parse(TestData.sampleXml(), TestData.DAY)
        assertEquals(BigDecimal.ONE, snapshot.perUnit("MDL"))
        assertEquals(listOf("MDL", "EUR", "USD"), snapshot.codes.take(3))
        assertEquals(41, snapshot.codes.size)
    }

    @Test
    fun `broken valute elements are skipped`() {
        val data = xml(
            "18.11.2026",
            valute("USD", "1", "18.3383"),
            valute("US", "1", "1.0"),        // не три буквы
            valute("MDL", "1", "1.0"),       // базовая валюта
            valute("AAA", "0", "1.0"),       // номинал 0
            valute("BBB", "1", "-5"),        // отрицательный курс
            valute("CCC", "1", "abc"),       // не число
            "<Valute><CharCode>DDD</CharCode><Nominal>1</Nominal></Valute>", // нет Value
            valute("EUR", "1", "20,1538"),   // запятая вместо точки
            valute("USD", "1", "99"),        // дубликат — побеждает первый
        )
        val rates = BnmXmlParser.parse(data, TestData.DAY).rates

        assertEquals(setOf("USD", "EUR"), rates.keys)
        assertEquals(BigDecimal("18.3383"), rates.getValue("USD").value)
        assertEquals(BigDecimal("20.1538"), rates.getValue("EUR").value)
    }

    @Test
    fun `missing date attribute falls back to requested date`() {
        val data = """<ValCurs>${valute("USD", "1", "18")}</ValCurs>""".toByteArray()
        assertEquals(TestData.DAY, BnmXmlParser.parse(data, TestData.DAY).date)
    }

    @Test
    fun `empty ValCurs gives empty rates`() {
        assertTrue(BnmXmlParser.parse(xml("18.11.2026"), TestData.DAY).rates.isEmpty())
    }

    @Test
    fun `malformed xml throws InvalidDataException`() {
        assertThrows(InvalidDataException::class.java) {
            BnmXmlParser.parse("<ValCurs><Valute>".toByteArray(), TestData.DAY)
        }
        assertThrows(InvalidDataException::class.java) {
            BnmXmlParser.parse("not xml at all".toByteArray(), TestData.DAY)
        }
    }

    @Test
    fun `unexpected root throws InvalidDataException`() {
        assertThrows(InvalidDataException::class.java) {
            BnmXmlParser.parse("<html><body>error</body></html>".toByteArray(), TestData.DAY)
        }
    }
}
