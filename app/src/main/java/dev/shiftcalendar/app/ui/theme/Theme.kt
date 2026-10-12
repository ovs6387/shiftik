package dev.shiftcalendar.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Тема приложения. [style] выбирает оформление из [themeSpecFor];
 * [darkTheme] — светлый/тёмный режим внутри оформления.
 */
@Composable
fun ShiftCalendarTheme(
    darkTheme: Boolean,
    style: AppThemeStyle,
    content: @Composable () -> Unit,
) {
    val spec = themeSpecFor(style)
    val colorScheme = if (darkTheme) spec.dark else spec.light
    // Системные панели под выбранную тему (важно при принудительной теме, отличной от системной).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = spec.shapes,
        typography = spec.typography,
        content = content,
    )
}
