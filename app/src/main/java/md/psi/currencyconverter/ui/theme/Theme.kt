package md.psi.currencyconverter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import md.psi.currencyconverter.domain.Trend

private val Light = lightColorScheme(
    primary = Color(0xFF2156B8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE7FF),
    onPrimaryContainer = Color(0xFF112D61),
    secondary = Color(0xFF465C7D),
    secondaryContainer = Color(0xFFE3EDFC),
    onSecondaryContainer = Color(0xFF233A63),
    tertiaryContainer = Color(0xFFFFEDCA),
    onTertiaryContainer = Color(0xFF5E3B08),
    background = Color(0xFFF2F6FC),
    onBackground = Color(0xFF132744),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF132744),
    surfaceVariant = Color(0xFFEAF0FA),
    onSurfaceVariant = Color(0xFF4E6078),
    surfaceContainer = Color(0xFFEEF3FB),
    surfaceContainerLow = Color(0xFFF7F9FD),
    surfaceContainerHigh = Color(0xFFE7EEFA),
    outline = Color(0xFF6A7B94),
    outlineVariant = Color(0xFFD5E0F0),
    error = Color(0xFFA42830),
    errorContainer = Color(0xFFFFE5E6),
    onErrorContainer = Color(0xFF77222A),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF122F65),
    primaryContainer = Color(0xFF203F75),
    onPrimaryContainer = Color(0xFFE0EBFF),
    secondary = Color(0xFFB0C5E4),
    secondaryContainer = Color(0xFF2A3D59),
    onSecondaryContainer = Color(0xFFDCE8FE),
    tertiaryContainer = Color(0xFF493514),
    onTertiaryContainer = Color(0xFFFFDFA6),
    background = Color(0xFF101827),
    onBackground = Color(0xFFE4EDFD),
    surface = Color(0xFF1A2639),
    onSurface = Color(0xFFE4EDFD),
    surfaceVariant = Color(0xFF25344D),
    onSurfaceVariant = Color(0xFFB9C7DD),
    surfaceContainer = Color(0xFF1C2A40),
    surfaceContainerLow = Color(0xFF172234),
    surfaceContainerHigh = Color(0xFF26364E),
    outline = Color(0xFF899BB6),
    outlineVariant = Color(0xFF3D516F),
    error = Color(0xFFFFB3B4),
    errorContainer = Color(0xFF52282F),
    onErrorContainer = Color(0xFFFFCDD0),
)

private val CurrencyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(15.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun trendColor(trend: Trend): Color = when (trend) {
    Trend.UP -> if (isSystemInDarkTheme()) Color(0xFF8EDDAD) else Color(0xFF17683F)
    Trend.DOWN -> MaterialTheme.colorScheme.error
    Trend.FLAT -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun CurrencyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, shapes = CurrencyShapes, content = content)
}
