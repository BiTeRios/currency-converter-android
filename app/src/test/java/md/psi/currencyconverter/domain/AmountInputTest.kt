package md.psi.currencyconverter.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class AmountInputTest {

    private fun valid(text: String): BigDecimal =
        (AmountInput.check(text) as? AmountCheck.Valid)?.value
            ?: throw AssertionError("«$text» должно приниматься, а получено ${AmountInput.check(text)}")

    private fun error(text: String): String =
        (AmountInput.check(text) as? AmountCheck.Invalid)?.message
            ?: throw AssertionError("«$text» должно отклоняться")

    @Test
    fun `accepted formats`() {
        assertEquals(BigDecimal("150"), valid("150"))
        assertEquals(BigDecimal("99.95"), valid("99,95"))
        assertEquals(BigDecimal("99.95"), valid("99.95"))
        assertEquals(BigDecimal("1500.50"), valid(" 1 500,50 "))
        assertEquals(BigDecimal("1500"), valid("1 500"))
        assertEquals(BigDecimal("0.01"), valid("0,01"))
        assertEquals(BigDecimal("1000000000000"), valid("1000000000000"))
    }

    @Test
    fun `empty input`() {
        assertEquals(AmountInput.EMPTY, error(""))
        assertEquals(AmountInput.EMPTY, error("   "))
    }

    @Test
    fun `letters and other garbage`() {
        listOf("abc", "12a", "1e5", "NaN", "Infinity", "--5", "1.2.3", "1,", ",5", "+5", "5-").forEach {
            assertEquals("ввод «$it»", AmountInput.NOT_A_NUMBER, error(it))
        }
    }

    @Test
    fun `zero and negative`() {
        assertEquals(AmountInput.NOT_POSITIVE, error("0"))
        assertEquals(AmountInput.NOT_POSITIVE, error("0,00"))
        assertEquals(AmountInput.NOT_POSITIVE, error("-10"))
    }

    @Test
    fun `too many decimals and too large`() {
        assertEquals(AmountInput.TOO_PRECISE, error("1,234"))
        assertEquals(AmountInput.TOO_LARGE, error("1000000000000.01"))
    }
}
