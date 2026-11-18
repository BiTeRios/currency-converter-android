package md.psi.currencyconverter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF1F5E4B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBDF),
    onPrimaryContainer = Color(0xFF062019),
    secondaryContainer = Color(0xFFE3ECE8),
    surface = Color(0xFFFBFCFB),
    surfaceVariant = Color(0xFFE6EDEA),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8FD3B8),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF0E4A3A),
    onPrimaryContainer = Color(0xFFCDEBDF),
    secondaryContainer = Color(0xFF2B3A35),
    surface = Color(0xFF111614),
    surfaceVariant = Color(0xFF26302C),
    error = Color(0xFFF2B8B5),
)

/** Цвета тенденции: рост — зелёный, падение — красный. */
object TrendColors {
    val up = Color(0xFF2E7D32)
    val down = Color(0xFFC62828)
    val flat = Color(0xFF6B7280)
}

@Composable
fun CurrencyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
