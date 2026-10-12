package dev.shiftcalendar.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shiftcalendar.app.R

/**
 * Оформления приложения. Индекс (ordinal) хранится в настройках (`opt_theme_style`).
 * Две темы из дизайн-систем Stitch: CLASSIC (Shift Rhythm) и PERIWINKLE (Periwinkle Shift System).
 */
enum class AppThemeStyle { CLASSIC, PERIWINKLE }

/** Полный набор токенов одного оформления. */
data class ThemeSpec(
    val light: androidx.compose.material3.ColorScheme,
    val dark: androidx.compose.material3.ColorScheme,
    val typography: Typography,
    val shapes: Shapes,
    /** Цвет-образец палитры для списка выбора оформления. */
    val preview: Color,
)

// ---------------- шрифты (variable TTF, ось wght) ----------------

@OptIn(ExperimentalTextApi::class)
private fun variableFontFamily(resId: Int): FontFamily = FontFamily(
    Font(resId, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(resId, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(resId, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(resId, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(resId, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/** Plus Jakarta Sans — фирменный шрифт обеих дизайн-систем Stitch. */
val PlusJakartaFamily: FontFamily = variableFontFamily(R.font.plus_jakarta_sans)

/** Типографика: крупные заголовки [headline], текст [body], подписи [label]. */
private fun appTypography(
    headline: FontFamily = PlusJakartaFamily,
    body: FontFamily = PlusJakartaFamily,
    label: FontFamily = PlusJakartaFamily,
): Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = headline, fontSize = 36.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold,
    ),
    headlineLarge = TextStyle(
        fontFamily = headline, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.2).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = headline, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.1).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = headline, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold,
    ),
    titleLarge = TextStyle(
        fontFamily = headline, fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = headline, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = headline, fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(fontFamily = body, fontSize = 15.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontFamily = body, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
    bodySmall = TextStyle(fontFamily = body, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(
        fontFamily = label, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = label, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = label, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp,
    ),
)

// ---------------- оформления ----------------

/** Classic = макеты Stitch (Tactile Warm Editorial): крем + светлые шалфейные пилюли. */
private val ClassicLight = lightColorScheme(
    primary = Color(0xFF1B3B22),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC9E4CB),
    onPrimaryContainer = Color(0xFF0E2814),
    inversePrimary = Color(0xFF9ED3AA),
    secondary = Color(0xFF576054),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDEFE0),
    onSecondaryContainer = Color(0xFF1A3622),
    tertiary = Color(0xFF3C2B00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF574000),
    onTertiaryContainer = Color(0xFFD7AA3F),
    error = Color(0xFFB93429),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE8E5),
    onErrorContainer = Color(0xFF8C1D13),
    background = Color(0xFFFAF7EE),
    onBackground = Color(0xFF1B1C18),
    surface = Color(0xFFFAF7EE),
    onSurface = Color(0xFF1B1C18),
    surfaceVariant = Color(0xFFF0EDE4),
    onSurfaceVariant = Color(0xFF4E544A),
    surfaceDim = Color(0xFFE2DFD4),
    surfaceBright = Color(0xFFFAF7EE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F3EC),
    surfaceContainer = Color(0xFFF0EDE4),
    surfaceContainerHigh = Color(0xFFE9E6DC),
    surfaceContainerHighest = Color(0xFFE2DFD4),
    outline = Color(0xFF8A9186),
    outlineVariant = Color(0xFFE7E4D8),
    inverseSurface = Color(0xFF2F372C),
    inverseOnSurface = Color(0xFFF3F1EA),
)

private val ClassicDark = darkColorScheme(
    primary = Color(0xFF9ED3AA),
    onPrimary = Color(0xFF00210E),
    primaryContainer = Color(0xFF1E5031),
    onPrimaryContainer = Color(0xFFB9EFC5),
    inversePrimary = Color(0xFF00361A),
    secondary = Color(0xFFB2CDB7),
    onSecondary = Color(0xFF082012),
    secondaryContainer = Color(0xFF344C3B),
    onSecondaryContainer = Color(0xFFCEEAD3),
    tertiary = Color(0xFFEFC052),
    onTertiary = Color(0xFF251A00),
    tertiaryContainer = Color(0xFF5B4300),
    onTertiaryContainer = Color(0xFFFFDF9D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191D18),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF191D18),
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC1C9BF),
    surfaceDim = Color(0xFF191D18),
    surfaceBright = Color(0xFF353A33),
    surfaceContainerLowest = Color(0xFF0E110D),
    surfaceContainerLow = Color(0xFF22261F),
    surfaceContainer = Color(0xFF262B23),
    surfaceContainerHigh = Color(0xFF31352E),
    surfaceContainerHighest = Color(0xFF3C4038),
    outline = Color(0xFF8C9388),
    outlineVariant = Color(0xFF414942),
    inverseSurface = Color(0xFFE2E3DD),
    inverseOnSurface = Color(0xFF30312D),
)

/** Periwinkle = Periwinkle Shift System (Stitch). Прохладный перванш #306EE8. */
private val PeriwinkleLight = lightColorScheme(
    primary = Color(0xFF0054CB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF306EE8),
    onPrimaryContainer = Color(0xFFFEFCFF),
    inversePrimary = Color(0xFFB1C5FF),
    secondary = Color(0xFF545F72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E3FA),
    onSecondaryContainer = Color(0xFF5A6578),
    tertiary = Color(0xFF7D5400),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9D6B00),
    onTertiaryContainer = Color(0xFFFFFCFF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF101B2F),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF101B2F),
    surfaceVariant = Color(0xFFD8E2FE),
    onSurfaceVariant = Color(0xFF424654),
    surfaceDim = Color(0xFFCFDAF5),
    surfaceBright = Color(0xFFF9F9FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F3FF),
    surfaceContainer = Color(0xFFE8EDFF),
    surfaceContainerHigh = Color(0xFFE0E8FF),
    surfaceContainerHighest = Color(0xFFD8E2FE),
    outline = Color(0xFF737785),
    outlineVariant = Color(0xFFC3C6D6),
    inverseSurface = Color(0xFF263045),
    inverseOnSurface = Color(0xFFEDF0FF),
)

private val PeriwinkleDark = darkColorScheme(
    primary = Color(0xFFB1C5FF),
    onPrimary = Color(0xFF001947),
    primaryContainer = Color(0xFF00419F),
    onPrimaryContainer = Color(0xFFDAE2FF),
    inversePrimary = Color(0xFF0054CB),
    secondary = Color(0xFFBCC7DD),
    onSecondary = Color(0xFF111C2C),
    secondaryContainer = Color(0xFF3C4759),
    onSecondaryContainer = Color(0xFFD8E3FA),
    tertiary = Color(0xFFFFBA42),
    onTertiary = Color(0xFF281800),
    tertiaryContainer = Color(0xFF614000),
    onTertiaryContainer = Color(0xFFFFDDAF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1626),
    onBackground = Color(0xFFE0E8FF),
    surface = Color(0xFF0F1626),
    onSurface = Color(0xFFE0E8FF),
    surfaceVariant = Color(0xFF424654),
    onSurfaceVariant = Color(0xFFC3C6D6),
    surfaceDim = Color(0xFF0F1626),
    surfaceBright = Color(0xFF343B4E),
    surfaceContainerLowest = Color(0xFF0A0F1E),
    surfaceContainerLow = Color(0xFF171C2E),
    surfaceContainer = Color(0xFF1B2133),
    surfaceContainerHigh = Color(0xFF252C3F),
    surfaceContainerHighest = Color(0xFF303748),
    outline = Color(0xFF8D92A3),
    outlineVariant = Color(0xFF424654),
    inverseSurface = Color(0xFFE0E8FF),
    inverseOnSurface = Color(0xFF263045),
)

// ---------------- формы ----------------

/** Classic (дизайн-система Stitch): мягкие «книжные» скругления. */
private val ClassicShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Periwinkle: округлее, full-pill элементы. */
private val PeriwinkleShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(48.dp),
)

// ---------------- спецификации ----------------

private val ClassicSpec = ThemeSpec(
    light = ClassicLight,
    dark = ClassicDark,
    typography = appTypography(),
    shapes = ClassicShapes,
    preview = Color(0xFF1B4D2E),
)

private val PeriwinkleSpec = ThemeSpec(
    light = PeriwinkleLight,
    dark = PeriwinkleDark,
    typography = appTypography(),
    shapes = PeriwinkleShapes,
    preview = Color(0xFF306EE8),
)

/** Токены оформления по стилю. */
fun themeSpecFor(style: AppThemeStyle): ThemeSpec = when (style) {
    AppThemeStyle.CLASSIC -> ClassicSpec
    AppThemeStyle.PERIWINKLE -> PeriwinkleSpec
}

/** Безопасный разбор индекса оформления из настроек (неизвестное значение → CLASSIC). */
fun appThemeStyleOf(index: Int): AppThemeStyle =
    AppThemeStyle.entries.getOrElse(index) { AppThemeStyle.CLASSIC }